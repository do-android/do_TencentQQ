package doext.implement;

import org.json.JSONObject;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.connect.UserInfo;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import core.DoServiceContainer;
import core.helper.jsonparse.DoJsonNode;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.app.do_TencentQQ_App;
import doext.define.do_TencentQQ_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_TencentQQ_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_TencentQQ_Model extends DoSingletonModule implements do_TencentQQ_IMethod {

	private Tencent mTencent;

	public do_TencentQQ_Model() throws Exception {
		super();
		do_TencentQQ_App.getInstance().setModuleTypeID(getTypeID());
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		// ...do something
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("login".equals(_methodName)) {
			this.login(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("getUserInfo".equals(_methodName)) {
			this.getUserInfo(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("logout".equals(_methodName)) {
			this.logout(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 获取用户信息；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void getUserInfo(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _token = _dictParas.getOneText("token", "");
		String _expires = _dictParas.getOneText("expires", "");
		String _openId = _dictParas.getOneText("openId", "");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (mTencent != null && mTencent.isSessionValid() && !TextUtils.isEmpty(_token) && !TextUtils.isEmpty(_expires) && !TextUtils.isEmpty(_openId)) {
			mTencent.setAccessToken(_token, _expires);
			mTencent.setOpenId(_openId);
			UserInfo mInfo = new UserInfo(_activity, mTencent.getQQToken());
			mInfo.getUserInfo(new GetUserInfoListener(_activity, _scriptEngine, _callbackFuncName));
		}

	}

	private class GetUserInfoListener implements IUiListener {

		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;
		private Activity activity;

		public GetUserInfoListener(Activity _activity, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
			this.activity = _activity;
		}

		@Override
		public void onCancel() {
		}

		@Override
		public void onComplete(Object response) {
			if (null == response) {
				Toast.makeText(activity, "获取用户信息失败", Toast.LENGTH_SHORT).show();
				return;
			}
			JSONObject jsonResponse = (JSONObject) response;
			if (null != jsonResponse && jsonResponse.length() == 0) {
				Toast.makeText(activity, "获取用户信息失败", Toast.LENGTH_SHORT).show();
				return;
			}
			invokeResult.setResultText(jsonResponse.toString());
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

		@Override
		public void onError(UiError e) {
			invokeResult.setError("code = " + e.errorCode + "\n\t message = " + e.errorMessage + "\n\t detail = " + e.errorDetail);
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

	}

	/**
	 * 使用qq登录；
	 * 
	 * @throws Exception
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void login(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _appId = _dictParas.getOneText("appId", "");
		if (TextUtils.isEmpty(_appId))
			throw new Exception("appId 不能为空");
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (mTencent == null) {
			mTencent = Tencent.createInstance(_appId, _activity.getApplicationContext());
		}
		if (!mTencent.isSessionValid()) {
			mTencent.login(_activity, "all", new MyLoginListener(_activity, _scriptEngine, _callbackFuncName));
		}
	}

	private class MyLoginListener implements IUiListener {

		private DoIScriptEngine scriptEngine;
		private String callbackFuncName;
		private DoInvokeResult invokeResult;
		private Activity activity;

		public MyLoginListener(Activity _activity, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
			this.scriptEngine = _scriptEngine;
			this.callbackFuncName = _callbackFuncName;
			invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
			this.activity = _activity;
		}

		@Override
		public void onCancel() {
			Toast.makeText(activity, "取消授权", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onComplete(Object response) {
			if (null == response) {
				Toast.makeText(activity, "登录失败", Toast.LENGTH_SHORT).show();
				return;
			}
			JSONObject jsonResponse = (JSONObject) response;
			if (null != jsonResponse && jsonResponse.length() == 0) {
				Toast.makeText(activity, "登录失败", Toast.LENGTH_SHORT).show();
				return;
			}
			invokeResult.setResultText(jsonResponse.toString());
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

		@Override
		public void onError(UiError e) {
			invokeResult.setError("code = " + e.errorCode + "\n\t message = " + e.errorMessage + "\n\t detail = " + e.errorDetail);
			scriptEngine.callback(callbackFuncName, invokeResult);
		}

	}

	/**
	 * 注销；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void logout(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		DoInvokeResult _invokeResult = new DoInvokeResult(do_TencentQQ_Model.this.getUniqueKey());
		try {
			Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
			mTencent.logout(_activity.getApplicationContext());
			_invokeResult.setResultBoolean(true);
		} catch (Exception ex) {
			_invokeResult.setResultBoolean(false);
		} finally {
			_scriptEngine.callback(_callbackFuncName, _invokeResult);
		}
	}
}