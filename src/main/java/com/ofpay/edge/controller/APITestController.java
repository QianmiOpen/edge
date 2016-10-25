/**
 * BaseController.java
 */
package com.ofpay.edge.controller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.alibaba.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.common.URL;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.ofpay.edge.InterfaceExecutor;
import com.ofpay.edge.InterfaceLoader;
import com.ofpay.edge.bean.JSONTreeNode;

/**
 * <p>
 * Controller
 * </p>
 * @author Angus
 * @date 2013-2-25
 * @version 1.0
 * @since 1.0
 */
@Controller
@RequestMapping(value = "/", produces = "text/plain;charset=UTF-8")
public class APITestController {
    private static final Logger logger = LoggerFactory.getLogger(APITestController.class);

    private static final String FOLDER_SPLIT = "/";

    /**
     * 根据ID查找子节点
     * @param treeNode
     * @param path 子节点ID,支持沿深度查询。例如：com/xxx/abc; 直接返回abc节点
     * @return
     */
    private JSONTreeNode findChildByText(JSONTreeNode treeNode, String path) {
        if (treeNode == null) {
            return null;
        }

        List<String> list = Arrays.asList(path.split(FOLDER_SPLIT));
        JSONTreeNode childNode = null;
        for (String shortPath : list) {

            childNode = treeNode.getChildByText(shortPath);
            if (childNode == null) {
                return null;
            } else {
                treeNode = childNode;
            }
        }

        return childNode;
    }

    /**
     * 创建package节点, 支持沿深度创建<br>
     * 例如：com/xxx/abc将创建三层节点: <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;parentNode <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|--com <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|--xxx <br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|--abc <br>
     * @param parentNode 父节点
     * @param packageName 包名，支持深度创建；
     * @param isGroup 是否group；根据此标记选择不同样式
     */
    private void addPackageNode(JSONTreeNode parentNode, String packageName, boolean isGroup) {

        String[] packageArr = packageName.split(FOLDER_SPLIT, 2);

        String nodeName = packageArr[0];
        String nodeId = UUID.randomUUID().toString();
        String fullPath = parentNode.getFullPath() + FOLDER_SPLIT + nodeId;
        String fullText = parentNode.getFullText() + FOLDER_SPLIT + nodeName;

        JSONTreeNode childNode = parentNode.getChildByText(nodeName); // 根据node text 获取node是否存在

        if (null == childNode) { // 若不存则创建node，并添加到children中
            childNode = new JSONTreeNode();
            childNode = new JSONTreeNode();
            childNode.setId(nodeId);
            // childNode.setId(groupPrefix + "_" + nodeId);
            childNode.setText(nodeName);
            childNode.setCls("package");
            if (isGroup) {
                childNode.setIconCls("icon-cmp");
            } else {
                childNode.setIconCls("icon-pkg");
            }
            childNode.setSingleClickExpand(true);
            childNode.setFullPath(fullPath);
            childNode.setFullText(fullText);
            parentNode.addChildren(childNode);
        }

        if (packageArr.length > 1) {
            addPackageNode(childNode, packageArr[1], false);
        } else {
            childNode.setIconCls("icon-cls");
        }
    }

    /**
     * 创建方法节点
     * @param classNode class节点
     * @param methods 方法名称数组
     * @param serviceKey 获取URL的serviceKey，方便界面根据方法节点获取对应的URL；
     */
    private void addMethodNode(JSONTreeNode classNode, String[] methods, String serviceKey) {

        for (String methodName : methods) {
            JSONTreeNode methodNode = new JSONTreeNode();
            String nodeId = UUID.randomUUID().toString();
            methodNode.setId(nodeId);
            methodNode.setText(methodName);
            methodNode.setFullPath(classNode.getFullPath() + FOLDER_SPLIT + nodeId);
            methodNode.setFullText(classNode.getFullText() + FOLDER_SPLIT + methodName);
            methodNode.setCls("cls");
            methodNode.setIconCls("icon-method");
            methodNode.setLeaf(true);
            methodNode.setIsClass(true);
            methodNode.setServiceKey(serviceKey);

            classNode.addChildren(methodNode);
        }
    }

    /**
     * 获取接口JsonTree
     * @return json数组
     */
    @RequestMapping("getApiList.do")
    public @ResponseBody
    String getApiList() {

        Set<String> serviceKeySet = InterfaceLoader.getRegistryProviderCache().keySet();

        JSONTreeNode apidocsNode = new JSONTreeNode();
        apidocsNode.setId("ApiList");
        apidocsNode.setText("ApiList");
        apidocsNode.setIconCls("icon-docs");
        apidocsNode.setFullPath("ApiList");
        apidocsNode.setFullText("ApiList");
        apidocsNode.setSingleClickExpand(true);

        // beanName格式：group/com.xxx.xxxProvider:version
        for (String serviceKey : serviceKeySet) {

            URL url = InterfaceLoader.getRandomRegisterCacheURL(serviceKey);

            String group = url.getParameter("group");
            String className = url.getPath();
            String version = url.getParameter("version");

            try {
                Class.forName(className); // 忽略上下文中不存在class
            } catch (ClassNotFoundException e) {
                logger.debug("can not found bean {} in context", className);
                continue;
            }

            String packageName = className.replace(".", FOLDER_SPLIT);

            if (!StringUtils.hasText(group)) {
                group = "NULL_GROUP";
            }
            packageName = group + FOLDER_SPLIT + packageName;

            if (StringUtils.hasText(version))
                packageName = packageName + ":" + version;

            this.addPackageNode(apidocsNode, packageName, StringUtils.hasText(group));

            JSONTreeNode classNode = this.findChildByText(apidocsNode, packageName);

            if (null != classNode) {
                this.addMethodNode(classNode, url.getParameter("methods").split(","), serviceKey);
            }

        }

        if (null == apidocsNode.getChildren() || apidocsNode.getChildren().size() == 0) {
            apidocsNode.setLeaf(true);
        }

        List<JSONTreeNode> parentList = new ArrayList<JSONTreeNode>();
        parentList.add(apidocsNode);

        String json = JSON.toJSONString(parentList);
        logger.debug("{}", json);

        return json;
    }

    /**
     * 执行测试
     * @param methodName 接口名
     * @param params 参数名
     * @return 结果
     */
    @RequestMapping("executeTest.do")
    public @ResponseBody
    String executeTest(@RequestParam String methodName, @RequestParam String params,
            @RequestParam(required = false) String serviceUrl) {

        Map<String, Object> result = new HashMap<String, Object>();

        String msg = "";
        String arr[] = methodName.split("@");
        String serviceKey = arr[0];
        String mth = arr[1];
        try {

            Object serviceBean = InterfaceLoader.getServiceBean(serviceKey, serviceUrl);
            if (serviceBean == null) {
                msg = "找不到服务Bean";
            } else {
                Method serviceMethod = InterfaceLoader.getServiceMethod(serviceBean, mth);
                if (serviceMethod == null) {
                    msg = "找不到服务Method";
                } else {
                    msg = InterfaceExecutor.execute(serviceBean, serviceMethod, JSON.parseArray(params));
                }
            }

        } catch (JSONException e) {
            msg = "参数格式错误;";
        } catch (InvocationTargetException e){
            msg = "InvocationTargetException:" + InterfaceExecutor.getStackTrace(e.getTargetException());

            if(e.getTargetException() instanceof RpcException){
                //RPC异常时，清除缓存的ReferenceConfig对象
                InterfaceLoader.destroyReference(serviceKey, serviceUrl);
            }
        } catch (Exception e) {
            msg = "调用接口异常;" + InterfaceExecutor.getStackTrace(e);
        }

        result.put("success", true);
        result.put("msg", msg);

        String json = JSON.toJSONString(result);
        logger.debug("{}", json);
        return json;
    }

    /**
     * 获取接口参数描述
     * @param serviceKey 接口名称 (xxxProvider.xxx)
     * @return 接口参数的Json描述
     */
    @RequestMapping("getParamDesc.do")
    public @ResponseBody
    String getParamDesc(@RequestParam String serviceKey, @RequestParam String methodName) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("success", true);
        result.put("paramDesc", InterfaceLoader.getParamDesc(serviceKey, methodName));

        String[] serviceUrls = InterfaceLoader.getServiceUrl(serviceKey);
        String[][] displayUrls = new String[0][0];
        if (serviceUrls != null) {
            displayUrls = new String[serviceUrls.length][2];
            for (int i = 0; i < serviceUrls.length; i++) {
                displayUrls[i][0] = serviceUrls[i];
                displayUrls[i][1] = serviceUrls[i];
            }
        }

        result.put("serviceUrls", displayUrls);

        String json = JSON.toJSONString(result);
        logger.debug("{}", json);
        return json;
    }
}