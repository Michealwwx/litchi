//-------------------------------------------------
// Litchi Game Server Framework
// Copyright(c) 2019 phantaci <phantacix@qq.com>
// MIT Licensed
//-------------------------------------------------
package litchi.core.dataconfig.parse;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import litchi.core.common.utils.StringUtils;
import litchi.core.dataconfig.ConfigAdapter;
import litchi.core.dataconfig.annotation.FieldName;
import litchi.core.dataconfig.annotation.IndexPK;

/**
 * @author Phil
 * Date:   2018/3/17
 */
public class JSONDataParser implements DataParser {
	protected static final Logger LOGGER = LoggerFactory.getLogger(JSONDataParser.class);

	private static String FILE_EXT_NAME = ".json";

	@Override
	public <T extends ConfigAdapter> List<T> parse(String text, Class<T> className) {
		List<T> list = new ArrayList<>();
		if (StringUtils.isBlank(text)) {
			return list;
		}
		Field[] declaredFields = className.getDeclaredFields();
		Map<String, Field> fieldNames = new HashMap<>();
		for (Field field : declaredFields) {
			if (field.isAnnotationPresent(FieldName.class) == false &&
					field.isAnnotationPresent(IndexPK.class) == false) {
				continue;
			}
			fieldNames.put(field.getName(), field);
		}

		JSONArray jsonArray = (JSONArray) JSON.parse(text);
		for (Object object : jsonArray) {

			try {
				T instance = className.newInstance();
				JSONObject json = (JSONObject) object;
				for (Entry<String, Field> entry : fieldNames.entrySet()) {
					try {
						String fieldName = entry.getKey();
						Field field = entry.getValue();
						Object value = json.get(fieldName);
						if (value == null) {
							LOGGER.warn("field not found in json file, class:{} fileName={}", className, fieldName);
							continue;
						}
						Object typeValue = getTypeValue(value.toString(), field);
						field.setAccessible(true);
						field.set(instance, typeValue);
						field.setAccessible(false);
					} catch (Exception e) {
						LOGGER.error("", e);
						LOGGER.error("class={} fieldName={} value={}", className, entry.getKey(), json.get(entry.getKey()));
					}
				}
				list.add(instance);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		return list;
	}

	private Object getTypeValue(String object, Field field) {
		Class<?> type = field.getType();
		if (type == int.class || type == Integer.class) {
			if (object.equals("")) {
				return 0;
			}
			try {
				return Integer.valueOf(getNumber(object));
			} catch (Exception e) {
				LOGGER.error("", e);
			}

		} else if (type == long.class || type == Long.class) {
			if (object.equals("")) {
				return 0L;
			}
			return Long.valueOf(getNumber(object));
		} else if (type == boolean.class || type == Boolean.class) {
			try {
				Byte boolNum = Byte.valueOf(object);
				return boolNum == 0 ? false : true;
			} catch (Exception e) {
				return Boolean.valueOf(object.toLowerCase());
			}
		} else if (type == String.class) {
			return object;
		} else if (type == Map.class) {
			String str = object.toString();
			if (str == null || str.equals("")) {
				return new HashMap<>();
			}
			@SuppressWarnings("unchecked")
			HashMap<Object, Object> map = JSON.parseObject(str, HashMap.class);
			HashMap dataMap = new HashMap();
			Type genericType = field.getGenericType();
			if(genericType instanceof ParameterizedType){   
				ParameterizedType pt = (ParameterizedType) genericType;
				//得到泛型里的class类型对象  
				Class<?> keyClazz = (Class<?>)pt.getActualTypeArguments()[0]; 
				Class<?> valueClazz = (Class<?>)pt.getActualTypeArguments()[1]; 
				for (Entry<Object, Object> entry : map.entrySet()) {
					Object key = null;
					Object value = null;
					if (keyClazz == Integer.class) {
						key = Integer.valueOf(entry.getKey().toString());
					} else {
						LOGGER.error("unsuport map key type. class={}", keyClazz);
					}
					if (valueClazz == Integer.class) {
						value = Integer.valueOf(entry.getValue().toString());
					} else if (valueClazz == String.class) {
						value = entry.getValue().toString();
					} else {
						LOGGER.error("unsuport map value type. class={}", valueClazz);
					}
					dataMap.put(key, value);
				}
			}
			return dataMap;
		} else if (type == List.class) {
			String str = object.toString();
			if (str == null || str.equals("")) {
				return new ArrayList<>();
			}
			@SuppressWarnings("unchecked")
			ArrayList<Object> list = JSON.parseObject(str, ArrayList.class);
			ArrayList dataList = new ArrayList();
			Type genericType = field.getGenericType();
			if(genericType instanceof ParameterizedType){   
				ParameterizedType pt = (ParameterizedType) genericType;
				//得到泛型里的class类型对象  
				Class<?> valueClazz = (Class<?>)pt.getActualTypeArguments()[0]; 
				for (Object value : list) {
					if (valueClazz == Integer.class) {
						value = Integer.valueOf(value.toString());
					} else if (valueClazz == String.class) {
						value = value.toString();
					} else {
						LOGGER.error("unsuport map value type. class={}", valueClazz);
					}
					dataList.add(value);
				}
			}
			return dataList;
		} else if (type == short.class || type == Short.class) {
			if (object.equals("")) {
				return (short) 0;
			}
			return Short.valueOf(getNumber(object));
		} else if (type == byte.class || type == Byte.class) {
			if (object.equals("")) {
				return (byte) 0;
			}
			return Byte.valueOf(getNumber(object));
		} else if (type == float.class || type == Float.class) {
			if (object.equals("")) {
				return 0f;
			}
			return Float.valueOf(object);
		} else if (type == double.class || type == Double.class) {
			if (object.equals("")) {
				return 0d;
			}
			return Double.valueOf(object);
		}
		throw new RuntimeException("not support config data type. type=" + type.toString());
	}

	private String getNumber(String str) {
		int index = str.indexOf(".");
		if (index > -1) {
			return str.substring(0, index);
		}
		return str;
	}

	@Override
	public String fileExtensionName() {
		return FILE_EXT_NAME;
	}
}
