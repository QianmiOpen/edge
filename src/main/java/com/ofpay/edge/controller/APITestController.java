/**
 * BaseController.java
 */
package com.ofpay.edge.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private JSONTreeNode findChild(JSONTreeNode treeNode, String id) {
        if (treeNode == null) {
            return null;
        }

        List<String> list = Arrays.asList(id.split("\\."));
        JSONTreeNode childNode = null;
        for (String shortId : list) {
            childNode = treeNode.getChild(shortId);
            if (childNode == null) {
                return null;
            } else {
                treeNode = childNode;
            }
        }

        return childNode;
    }

    private void addPackageNode(JSONTreeNode parentNode, String packageName) {

        String[] packageArr = packageName.split("\\.", 2);

        String nodeName = packageArr[0];
        String nodeId = nodeName;
        String fullPath = parentNode.getFullPath() + "/" + nodeId;

        JSONTreeNode childNode = parentNode.getChild(nodeId); // 根据ID获取node是否存在

        if (null == childNode) { // 若不存则创建node，并添加到children中
            childNode = new JSONTreeNode();
            childNode = new JSONTreeNode();
            childNode.setId(nodeId);
            childNode.setText(nodeName);
            childNode.setCls("package");
            childNode.setIconCls("icon-pkg");
            childNode.setSingleClickExpand(true);
            childNode.setFullPath(fullPath);
            parentNode.addChildren(childNode);
        }

        if (packageArr.length > 1) {
            addPackageNode(childNode, packageArr[1]);
        }
    }

    private JSONTreeNode addClassNode(JSONTreeNode packageNode, String className) {
        JSONTreeNode classNode = new JSONTreeNode();
        classNode = new JSONTreeNode();
        classNode = new JSONTreeNode();
        classNode.setId(className);
        classNode.setText(className);
        classNode.setCls("package");
        classNode.setIconCls("icon-cls");
        classNode.setSingleClickExpand(true);
        classNode.setFullPath(packageNode.getFullPath() + "/" + className);

        packageNode.addChildren(classNode);

        return classNode;
    }

    private void addMethodNode(JSONTreeNode classNode, String[] methods, String serviceKey) {

        for (String methodName : methods) {
            JSONTreeNode methodNode = new JSONTreeNode();
            methodNode.setId(methodName);
            methodNode.setText(methodName);
            methodNode.setFullPath(classNode.getFullPath() + "/" + methodName);
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

        Set<String> serviceKeySet = InterfaceLoader.REGISTRY_PROVIDER_CACHE.keySet();

        JSONTreeNode apidocsNode = new JSONTreeNode();
        apidocsNode.setId("apidocs");
        apidocsNode.setText("API Lists");
        apidocsNode.setIconCls("icon-docs");
        apidocsNode.setFullPath("apidocs");
        apidocsNode.setSingleClickExpand(true);

        // beanName格式：group/com.xxx.xxxProvider:version
        for (String serviceKey : serviceKeySet) {

            URL url = InterfaceLoader.getRandomRegisterCacheURL(serviceKey);

            String group = "";
            String className = "";
            String version = "";

            String strArr1[] = serviceKey.split("/");
            if (strArr1.length > 1) {
                group = strArr1[0];
                className = strArr1[1];
            } else {
                className = strArr1[0];
            }

            String[] strArr2 = className.split(":");
            if (strArr2.length > 1) {
                className = strArr2[0];
                version = strArr2[1];
            }

            String packageName = className.substring(0, className.lastIndexOf("."));
            String beanName = className.substring(className.lastIndexOf(".") + 1);

            if (StringUtils.hasText(version)) {
                beanName = beanName + ":" + version;
            }

            if (StringUtils.hasText(group)) {
                JSONTreeNode groupNode = new JSONTreeNode();
                groupNode.setId(group);
                groupNode.setText(group);
                groupNode.setCls("package");
                groupNode.setIconCls("icon-cmp"); // icon-cmp
                groupNode.setSingleClickExpand(true);

                this.addPackageNode(groupNode, packageName);

                JSONTreeNode packageNode = this.findChild(groupNode, packageName);

                if (null != packageNode) {
                    JSONTreeNode classNode = this.addClassNode(packageNode, beanName);
                    this.addMethodNode(classNode, url.getParameter("methods").split(","), serviceKey);
                }

                apidocsNode.addChildren(groupNode);
            } else {

                this.addPackageNode(apidocsNode, packageName);
                JSONTreeNode packageNode = this.findChild(apidocsNode, packageName);

                if (null != packageNode) {
                    JSONTreeNode classNode = this.addClassNode(packageNode, beanName);
                    this.addMethodNode(classNode, url.getParameter("methods").split(","), serviceKey);
                }
            }

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
        try {
            String arr[] = methodName.split("@");
            String serviceKey = arr[0];
            String mth = arr[1];

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
        String[][] displayUrls = new String[serviceUrls.length][2];
        for (int i = 0; i < serviceUrls.length; i++) {
            displayUrls[i][0] = serviceUrls[i];
            displayUrls[i][1] = serviceUrls[i];
        }

        result.put("serviceUrls", displayUrls);

        String json = JSON.toJSONString(result);
        logger.debug("{}", json);
        return json;
    }
}