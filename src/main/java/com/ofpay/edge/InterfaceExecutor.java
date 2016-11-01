/**
 * InterfaceExecutor.java
 */
package com.ofpay.edge;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.List;


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

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 接口执行方法
     * @param target Service Bean对象
     * @param method Service Method
     * @param json 方法入参信息，采用Json描述
     * @return 调用结果
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String execute(Object target, Method method, String json) throws Exception {
        Object result;

        try {
            Object[] params = convertParams(json, method.getParameterTypes());
            result = method.invoke(target, params);
        } catch (IllegalArgumentException e) {
            result = e.getMessage();
        } catch (Exception e) {
            throw e;
        }

        return JSON.toJSONString(result, SerializerFeature.QuoteFieldNames, SerializerFeature.WriteMapNullValue,
                SerializerFeature.SortField);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object[] convertParams(String json, Class<?>[] paramTypes) throws Exception {
        Object[] paramArr;

        if (json == null || json.length() == 0) {
            paramArr = new Object[] {};
        } else {
            try {
                List inputParamList = objectMapper.readValue(json, List.class);
                paramArr = inputParamList.toArray();
            } catch (Exception ex) {
                log.warn("解析异常参数失败，json:{}", json, ex);
                throw new JSONException("解析异常参数失败，json: " + json);
            }
        }

        if (paramTypes.length != paramArr.length) {
            log.warn("参数值与参数类型个数不匹配, json:{}", json);
            throw new IllegalArgumentException("参数错误，传入的参数个数与接口不匹配");
        }

        Object[] params = new Object[paramArr.length];

        for (int i = 0; i < paramArr.length; i++) {
            Object param = paramArr[i];
            Class<?> paramType = paramTypes[i];

            params[i] = objectMapper.readValue(objectMapper.writeValueAsString(param), paramType);

//            params[i] = TypeUtils.cast(param, paramType, ParserConfig.getGlobalInstance());
//            if (InterfaceLoader.isWrapClass(paramType)) {
//                params[i] = ConvertUtils.convert(param, paramType);
//            } else if (paramType.isEnum()) {
//                if (param instanceof String) {
//                    String name = (String) param;
//                    if (name.length() == 0) {
//                        params[i] = null;
//                    } else {
//                        params[i] = Enum.valueOf((Class<? extends Enum>) paramType, name);
//                    }
//                } else if (param instanceof Number) {
//                    int ordinal = ((Number) param).intValue();
//
//                    Method mt = paramType.getMethod("values");
//                    Object[] values = (Object[]) mt.invoke(null);
//                    for (Object value : values) {
//                        Enum e = (Enum) value;
//                        if (e.ordinal() == ordinal) {
//                            params[i] = e;
//                        }
//                    }
//                }
//            } else if (param instanceof JSONObject) {
//                params[i] = JSON.toJavaObject((JSONObject) param, paramType);
//            }
        }
        return params;
    }

    /**
     * 将Throwable转换为字符串描述的堆栈信息
     * @param throwable 异常信息
     */
    public static String getStackTrace(Throwable throwable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        throwable.printStackTrace(printWriter);
        return result.toString();
    }

}
