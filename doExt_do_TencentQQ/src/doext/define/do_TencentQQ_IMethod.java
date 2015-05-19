package doext.define;

import org.json.JSONObject;
import core.interfaces.DoIScriptEngine;

/**
 * 声明自定义扩展组件方法
 */
public interface do_TencentQQ_IMethod {
	void getUserInfo(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception;

	void login(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception;

	void logout(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception;
	
	void shareToQQ(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception;
	
	void shareToQzone(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception;
}