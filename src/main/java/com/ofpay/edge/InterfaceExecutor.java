/**
 * InterfaceExecutor.java
 */
package com.ofpay.edge;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.ConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * <p>
 * 接口执行类
 * </p>
 * @author Angus
 * @date 2013-3-11
 * @version 1.0
 * @since 1.0
 */
public class InterfaceExecutor {
    private static Logger log = LoggerFactory.getLogger(InterfaceExecutor.class);

    /**
     * 接口执行方法
     * @param intfName 接口名，根据此名字获取对应的bean对象
     * @param methodName 方法名，根据此名字获取要调用的方法
     * @param inputParamArray 方法入参信息，采用Json描述
     * @return 调用结果
     */
    public static String execute(String intfName, String methodName, JSONArray inputParamArray) {
        String result = null;

        Object target = InterfaceLoader.allBeanMap.get(intfName);
        Method method = InterfaceLoader.allMethodMap.get(intfName + "." + methodName);

        if (null == target || null == method) {
            log.warn("未找到{}.{}接口", new Object[] { intfName, methodName });
            return "未找到此接口";
        }

        Class<?>[] paramTypes = method.getParameterTypes();

        try {
            Object[] params = null;

            if (paramTypes != null && paramTypes.length > 0) {

                if (inputParamArray == null || paramTypes.length != inputParamArray.size()) {
                    log.warn("调用{}.{}接口参数值与参数个数不匹配, paramList:{}",
                            new Object[] { intfName, methodName, inputParamArray });
                    return "参数错误，出入的参数个数与接口不匹配";
                }

                Object[] paramArr = inputParamArray.toArray();
                params = new Object[inputParamArray.size()];

                for (int i = 0; i < paramArr.length; i++) {
                    Object param = paramArr[i];
                    Class<?> paramType = paramTypes[i];

                    if (InterfaceLoader.isWrapClass(paramType)) {
                        params[i] = ConvertUtils.convert(param, paramType);
                    } else if (param instanceof JSONObject) {
                        params[i] = JSON.toJavaObject((JSONObject) param, paramType);
                    }
                }
            }

            Object response = method.invoke(target, params);
            result = JSON.toJSONString(response, SerializerFeature.QuoteFieldNames, SerializerFeature.UseSingleQuotes,
                    SerializerFeature.WriteMapNullValue, SerializerFeature.SortField);

        } catch (Exception e) {
            result = "调用接口异常\r\n" + getStackTrace(e);
            log.error("{}.{}接口调用发生异常, paramMap:{}", new Object[] { intfName, methodName, inputParamArray, e });
        }

        return result;
    }

    /**
     * 将Throwable转换为字符串描述的堆栈信息
     * @param throwable 异常信息
     * @return
     */
    public static String getStackTrace(Throwable throwable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);
        return result.toString();
    }

}
