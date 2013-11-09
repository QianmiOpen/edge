/**
 * BaseController.java
 */
package com.ofpay.edge.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

    /**
     * 获取接口JsonTree
     * @return json数组
     */
    @RequestMapping("getApiList.do")
    public @ResponseBody
    String getApiList() {

        Set<String> beanNameSet = InterfaceLoader.allBeanMap.keySet();
        Set<String> methodNameSet = InterfaceLoader.allMethodMap.keySet();

        JSONTreeNode parentNode = new JSONTreeNode();
        parentNode.setId("apidocs");
        parentNode.setText("API Lists");
        parentNode.setIconCls("icon-docs");
        parentNode.setSingleClickExpand(true);

        List<JSONTreeNode> classList = new ArrayList<JSONTreeNode>();
        for (String beanName : beanNameSet) {

            JSONTreeNode classNode = new JSONTreeNode();
            classNode.setId(beanName);
            classNode.setText(beanName);
            classNode.setCls("package");
            classNode.setIconCls("icon-pkg");
            classNode.setSingleClickExpand(true);

            List<JSONTreeNode> methodList = new ArrayList<JSONTreeNode>();
            for (String methodName : methodNameSet) {

                if (methodName.startsWith(beanName)) {
                    JSONTreeNode methodNode = new JSONTreeNode();
                    methodNode.setId(methodName);
                    int len = methodName.split("\\.").length;
                    methodNode.setText(methodName.split("\\.")[len - 1]);
                    methodNode.setCls("cls");
                    methodNode.setIconCls("icon-cls");
                    methodNode.setLeaf(true);
                    methodNode.setIsClass(true);
                    methodList.add(methodNode);
                }
            }
            classNode.setChildren(methodList);
            classList.add(classNode);
        }

        parentNode.setChildren(classList);

        List<JSONTreeNode> parentList = new ArrayList<JSONTreeNode>();
        parentList.add(parentNode);

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
    String executeTest(@RequestParam String methodName, @RequestParam String params) {

        Map<String, Object> result = new HashMap<String, Object>();

        String msg = "";
        try {
            int idx = methodName.lastIndexOf(".");
            String cls = methodName.substring(0, idx);
            String mth = methodName.substring(idx + 1);
            msg = InterfaceExecutor.execute(cls, mth, JSON.parseArray(params));
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
     * @param methodName 接口名称 (xxxProvider.xxx)
     * @return 接口参数的Json描述
     */
    @RequestMapping("getParamDesc.do")
    public @ResponseBody
    String getParamDesc(@RequestParam String methodName) {

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("success", true);
        result.put("msg", InterfaceLoader.allMethodMapParamDesc.get(methodName));

        String json = JSON.toJSONString(result);
        logger.debug("{}", json);
        return json;
    }
}