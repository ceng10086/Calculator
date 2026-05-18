package com.example.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.calculator.InputItem.InputType;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private TextView mShowResultTv;  //显示结果
	private TextView mShowInputTv;   //显示输入的字符
	private Button mCBtn;
	private Button mDelBtn;
	private Button mAddBtn;
	private Button mSubBtn;
	private Button mMultiplyBtn;
	private Button mDividebtn;
	private Button mZeroButton;
	private Button mOnebtn;
	private Button mTwoBtn;
	private Button mThreeBtn;
	private Button mFourBtn;
	private Button mFiveBtn;
	private Button mSixBtn;
	private Button mSevenBtn;
	private Button mEightBtn;
	private Button mNineBtn;
	private Button mPointtn;
	private Button mEqualBtn;
	private HashMap<View,String> map; //将View和String映射起来
	private List<InputItem> mInputList; //定义记录每次输入的数
	private int mLastInputstatus = INPUT_NUMBER; //记录上一次输入状态
	public static final int INPUT_NUMBER = 1;
	public static final int INPUT_POINT = 0;
	public static final int INPUT_OPERATOR = -1;
	public static final int END = -2;
	public static final int ERROR= -3;

	// Handler message types. 使用大于 OP_DIV(=4) 的值，避免与 Mathservice 回传的 what 冲突。
	public static final int SHOW_RESULT_DATA = 100;
	public static final int MSG_NEXT_STEP = 101;

	// 四则混合运算的两个阶段：高优先级（*, /），低优先级（+, -）
	private static final int PHASE_HIGH = 1;
	private static final int PHASE_LOW = 2;

	public static final String nan = "NaN";
	public static final String infinite = "∞";
	public static final String TAG = "calculator";

	// 异步计算状态
	private int mPhase = PHASE_HIGH;
	private boolean mComputing = false;
	private int mCurrentTaskId = 0;
	private int mPendingTaskId = -1;
	private int mPendingOpIdx = -1;
	private int mPendingOp = 0;
	private boolean mPendingLeftWasInt;
	private boolean mPendingRightWasInt;
	private Messenger mMessenger;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			Log.i(TAG, "Handler receive msg what=" + msg.what + " arg1=" + msg.arg1
					+ " PID=" + Process.myPid()
					+ " TID=" + Process.myTid()
					+ " ThreadName=" + Thread.currentThread().getName());

			if (msg.what == SHOW_RESULT_DATA) {
				finalizeComputation();
			} else if (msg.what == MSG_NEXT_STEP) {
				stepNext();
			} else if (msg.what >= Mathservice.OP_ADD && msg.what <= Mathservice.OP_DIV) {
				handleServiceResult(msg);
			}
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mMessenger = new Messenger(mHandler);
		Log.i(TAG, "MainActivity onCreate"
				+ " PID=" + Process.myPid()
				+ " TID=" + Process.myTid()
				+ " ThreadName=" + Thread.currentThread().getName());
		initView();
		initData();
	}
	/**
	 * 初始化view
	 */
	private void initView() {
		mShowResultTv = (TextView) this.findViewById(R.id.show_result_tv);
		mShowInputTv = (TextView)this.findViewById(R.id.show_input_tv);
		mCBtn = (Button)this.findViewById(R.id.c_btn);
		mDelBtn= (Button)this.findViewById(R.id.del_btn);
		mAddBtn= (Button)this.findViewById(R.id.add_btn);
		mMultiplyBtn= (Button)this.findViewById(R.id.multiply_btn);
		mDividebtn= (Button)this.findViewById(R.id.divide_btn);
		mZeroButton = (Button)this.findViewById(R.id.zero_btn);
		mOnebtn= (Button)this.findViewById(R.id.one_btn);
		mTwoBtn= (Button)this.findViewById(R.id.two_btn);
		mThreeBtn= (Button)this.findViewById(R.id.three_btn);
		mFourBtn= (Button)this.findViewById(R.id.four_btn);
		mFiveBtn= (Button)this.findViewById(R.id.five_btn);
		mSixBtn= (Button)this.findViewById(R.id.six_btn);
		mSevenBtn= (Button)this.findViewById(R.id.seven_btn);
		mEightBtn= (Button)this.findViewById(R.id.eight_btn);
		mNineBtn= (Button)this.findViewById(R.id.nine_btn);
		mPointtn= (Button)this.findViewById(R.id.point_btn);
		mEqualBtn= (Button)this.findViewById(R.id.equal_btn);
		mSubBtn = (Button)this.findViewById(R.id.sub_btn);
		setOnClickListener();//调用监听事件

	}
	/**
	 * 初始化数据
	 */
	private void initData() {
		if(map == null)
			map = new HashMap<View, String>();
		map.put(mAddBtn,getResources().getString(R.string.add));
		map.put(mMultiplyBtn,getResources().getString(R.string.multply));
		map.put(mDividebtn,getResources().getString(R.string.divide));
		map.put(mSubBtn, getResources().getString(R.string.sub));
		map.put(mZeroButton ,getResources().getString(R.string.zero));
		map.put(mOnebtn,getResources().getString(R.string.one));
		map.put(mTwoBtn,getResources().getString(R.string.two));
		map.put(mThreeBtn,getResources().getString(R.string.three));
		map.put(mFourBtn,getResources().getString(R.string.four));
		map.put(mFiveBtn,getResources().getString(R.string.five));
		map.put(mSixBtn,getResources().getString(R.string.six));
		map.put(mSevenBtn,getResources().getString(R.string.seven));
		map.put(mEightBtn,getResources().getString(R.string.eight));
		map.put(mNineBtn,getResources().getString(R.string.nine));
		map.put(mPointtn,getResources().getString(R.string.point));
		map.put(mEqualBtn,getResources().getString(R.string.equal));
		mInputList = new ArrayList<InputItem>();
		mShowResultTv.setText("");
		clearAllScreen();
	}

	/**
	 * 设置监听事件
	 */
	private void setOnClickListener() {
		mCBtn.setOnClickListener(this);
		mDelBtn.setOnClickListener(this);
		mAddBtn.setOnClickListener(this);
		mMultiplyBtn.setOnClickListener(this);
		mDividebtn.setOnClickListener(this);
		mSubBtn.setOnClickListener(this);
		mZeroButton.setOnClickListener(this);
		mOnebtn.setOnClickListener(this);
		mTwoBtn.setOnClickListener(this);
		mThreeBtn.setOnClickListener(this);
		mFourBtn.setOnClickListener(this);
		mFiveBtn.setOnClickListener(this);
		mSixBtn.setOnClickListener(this);
		mSevenBtn.setOnClickListener(this);
		mEightBtn.setOnClickListener(this);
		mNineBtn.setOnClickListener(this);
		mPointtn.setOnClickListener(this);
		mEqualBtn.setOnClickListener(this);
	}

	/**
	 * 点击事件
	 */
	@Override
	public void onClick(View arg0) {
        // 异步计算过程中忽略按键，避免状态混乱
        if (mComputing) {
            return;
        }
        int id = arg0.getId();
        if (id == R.id.c_btn) {
            clearAllScreen();
        } else if (id == R.id.del_btn) {
            back();
        } else if (id == R.id.point_btn) {
            inputPoint(arg0);
        } else if (id == R.id.equal_btn) {
            operator();
        } else if (id == R.id.add_btn || id == R.id.sub_btn || id == R.id.multiply_btn || id == R.id.divide_btn) {
            inputOperator(arg0);
        } else {
            inputNumber(arg0);
        }
	}
	/**
	 * 点击 = 后启动异步运算流程：先处理高优先级（*, /），再处理低优先级（+, -）。
	 * 每一步运算都通过 Mathservice 的 4 个子线程完成，结果通过 Handler 回到主线程。
	 */
	private void operator() {
		if(mLastInputstatus == END ||mLastInputstatus == ERROR || mLastInputstatus == INPUT_OPERATOR|| mInputList.size()==1){
			return;
		}
		mComputing = true;
		mShowResultTv.setText("");
		startAnim();
		mPhase = PHASE_HIGH;
		Log.i(TAG, "operator() start async compute"
				+ " PID=" + Process.myPid()
				+ " TID=" + Process.myTid());
		// 略作延时以让动画启动，再开始计算
		mHandler.sendEmptyMessageDelayed(MSG_NEXT_STEP, 50);
	}

	/**
	 * 推进下一步运算：根据当前阶段在 mInputList 中找到第一个对应优先级的运算符，
	 * 若有则交给 Mathservice 计算；若没有则切换阶段或结束。
	 */
	private void stepNext() {
		if (mLastInputstatus == ERROR) {
			mHandler.sendEmptyMessageDelayed(SHOW_RESULT_DATA, 250);
			return;
		}
		int idx = findIndexOfPhaseOperator(mPhase);
		if (idx == -1) {
			if (mPhase == PHASE_HIGH) {
				mPhase = PHASE_LOW;
				stepNext();
			} else {
				mHandler.sendEmptyMessageDelayed(SHOW_RESULT_DATA, 250);
			}
			return;
		}
		sendComputeRequest(idx);
	}

	private int findIndexOfPhaseOperator(int phase) {
		if (mInputList == null || mInputList.size() <= 1) {
			return -1;
		}
		String add = getResources().getString(R.string.add);
		String sub = getResources().getString(R.string.sub);
		String mul = getResources().getString(R.string.multply);
		String div = getResources().getString(R.string.divide);
		for (int i = 0; i < mInputList.size(); i++) {
			String s = mInputList.get(i).getInput();
			if (phase == PHASE_HIGH && (mul.equals(s) || div.equals(s))) {
				return i;
			}
			if (phase == PHASE_LOW && (add.equals(s) || sub.equals(s))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 把第 idx 处的二元运算交给 Mathservice 执行。
	 */
	private void sendComputeRequest(int idx) {
		InputItem opItem = mInputList.get(idx);
		InputItem leftItem = mInputList.get(idx - 1);
		InputItem rightItem = mInputList.get(idx + 1);
		String opStr = opItem.getInput();
		int op;
		if (getResources().getString(R.string.add).equals(opStr)) {
			op = Mathservice.OP_ADD;
		} else if (getResources().getString(R.string.sub).equals(opStr)) {
			op = Mathservice.OP_SUB;
		} else if (getResources().getString(R.string.multply).equals(opStr)) {
			op = Mathservice.OP_MUL;
		} else {
			op = Mathservice.OP_DIV;
		}

		double a = Double.parseDouble(leftItem.getInput());
		double b = Double.parseDouble(rightItem.getInput());

		mCurrentTaskId++;
		mPendingTaskId = mCurrentTaskId;
		mPendingOpIdx = idx;
		mPendingOp = op;
		mPendingLeftWasInt = leftItem.getType() == InputType.INT_TYPE;
		mPendingRightWasInt = rightItem.getType() == InputType.INT_TYPE;

		Intent intent = new Intent(this, Mathservice.class);
		intent.putExtra(Mathservice.EXTRA_A, a);
		intent.putExtra(Mathservice.EXTRA_B, b);
		intent.putExtra(Mathservice.EXTRA_OP, op);
		intent.putExtra(Mathservice.EXTRA_TASK_ID, mPendingTaskId);
		intent.putExtra(Mathservice.EXTRA_MESSENGER, mMessenger);

		Log.i(TAG, "sendComputeRequest a=" + a + " b=" + b + " op=" + op
				+ " taskId=" + mPendingTaskId
				+ " PID=" + Process.myPid()
				+ " TID=" + Process.myTid());

		startService(intent);
	}

	/**
	 * 处理 Mathservice 中某个子线程通过 Handler 回传的运算结果。
	 * 一次请求会有 4 个子线程回传，本方法只采用 op 匹配的那一个，其余忽略。
	 */
	private void handleServiceResult(Message msg) {
		if (msg.arg1 != mPendingTaskId) {
			return; // 过期消息
		}
		if (msg.what != mPendingOp) {
			return; // 非匹配子线程的结果，丢弃
		}

		Bundle data = msg.getData();
		double result = data.getDouble(Mathservice.DATA_RESULT);
		boolean error = data.getBoolean(Mathservice.DATA_ERROR);

		if (error) {
			// 除零错误
			mLastInputstatus = ERROR;
			double a = Double.parseDouble(mInputList.get(mPendingOpIdx - 1).getInput());
			if (a == 0) {
				clearScreen(new InputItem(nan, InputType.ERROR));
			} else {
				clearScreen(new InputItem(infinite, InputType.ERROR));
			}
			mHandler.sendEmptyMessageDelayed(SHOW_RESULT_DATA, 250);
			return;
		}

		// 根据操作数类型决定结果是 INT 还是 DOUBLE
		boolean wholeNumber = !Double.isInfinite(result) && result == Math.floor(result);
		int resultType;
		String resultStr;
		if (mPendingLeftWasInt && mPendingRightWasInt && wholeNumber) {
			resultType = InputType.INT_TYPE;
			resultStr = String.valueOf((long) result);
		} else {
			resultType = InputType.DOUBLE_TYPE;
			resultStr = String.valueOf(result);
		}

		mInputList.set(mPendingOpIdx - 1, new InputItem(resultStr, resultType));
		mInputList.remove(mPendingOpIdx + 1);
		mInputList.remove(mPendingOpIdx);

		Log.i(TAG, "applied result=" + resultStr + " remaining=" + mInputList.size()
				+ " PID=" + Process.myPid() + " TID=" + Process.myTid());

		mHandler.sendEmptyMessage(MSG_NEXT_STEP);
	}

	private void finalizeComputation() {
		if (mInputList != null && mInputList.size() > 0) {
			mShowResultTv.setText(mShowInputTv.getText());
			mShowInputTv.setText(mInputList.get(0).getInput());
			clearScreen(mInputList.get(0));
		}
		mComputing = false;
		Log.i(TAG, "finalizeComputation done"
				+ " PID=" + Process.myPid()
				+ " TID=" + Process.myTid());
	}

	private void startAnim(){
		mShowInputTv.setText(mShowInputTv.getText()+getResources().getString(R.string.equal));
		Animation anim = AnimationUtils.loadAnimation(this, R.anim.screen_anim);
		mShowInputTv.startAnimation(anim);
	}
	/**
	 * 输入点
	 * @param view
	 */
	private void inputPoint(View view) {
		if(mLastInputstatus == INPUT_POINT){
			return;
		}
		if(mLastInputstatus == END || mLastInputstatus == ERROR){
			clearInputScreen();
		}
		String key = map.get(view);
		String input = mShowInputTv.getText().toString();
		if(mLastInputstatus == INPUT_OPERATOR){
			input = input+"0";
		}
		mShowInputTv.setText(input+key);
		addInputList(INPUT_POINT, key);
	}
	/**
	 * 输入数字
	 * @param view
	 */
	private void inputNumber(View view){
		if(mLastInputstatus == END || mLastInputstatus == ERROR){
			clearInputScreen();
		}
		String key = map.get(view);
		Log.i(TAG,"inputNumber key="+ key);

		if("0".equals(mShowInputTv.getText().toString())){
			mShowInputTv.setText(key);
		}else{
		mShowInputTv.setText(mShowInputTv.getText() + key);
		}
		addInputList(INPUT_NUMBER, key);
	}
	/**
	 * 输入运算符
	 * @param view
	 */
	private void inputOperator(View view) {
		if(mLastInputstatus == INPUT_OPERATOR || mLastInputstatus == ERROR){
			return;
		}
		if(mLastInputstatus == END){
			mLastInputstatus = INPUT_NUMBER;
		}

		String key = map.get(view);
		if("0".equals(mShowInputTv.getText().toString())){
			mShowInputTv.setText("0"+key);
			mInputList.set(0,new InputItem("0",InputItem.InputType.INT_TYPE));
		}else{
		mShowInputTv.setText(mShowInputTv.getText() + key);
		}
		addInputList(INPUT_OPERATOR, key);
	}
	/**
	 * 回退操作
	 */
	private void back() {
		if(mLastInputstatus == ERROR){
			clearInputScreen();
		}
		String str = mShowInputTv.getText().toString();
		if(str.length() != 1){
			mShowInputTv.setText(str.substring(0, str.length()-1));
			backList();
		}else{
			mShowInputTv.setText(getResources().getString(R.string.zero));
			clearScreen(new InputItem("",InputItem.InputType.INT_TYPE));
		}
	}
	/**
	 * 回退InputList操作
	 */
	private void backList() {
		InputItem item = mInputList.get(mInputList.size() - 1);
		if (item.getType() == InputItem.InputType.INT_TYPE) {
			//获取到最后一个item，并去掉最后一个字符
			String input = item.getInput().substring(0,
					item.getInput().length() - 1);
			//如果截完了，则移除这个item，并将当前状态改为运算操作符
			if ("".equals(input)) {
				mInputList.remove(item);
				mLastInputstatus = INPUT_OPERATOR;
			} else {
				//否则设置item为截取完的字符串，并将当前状态改为number
				item.setInput(input);
				mLastInputstatus = INPUT_NUMBER;
			}
			//如果item是运算操作符，则移除
		} else if (item.getType() == InputItem.InputType.OPERATOR_TYPE) {
			mInputList.remove(item);
			if (mInputList.get(mInputList.size() - 1).getType() == InputItem.InputType.INT_TYPE) {
				mLastInputstatus = INPUT_NUMBER;
			} else {
				mLastInputstatus = INPUT_POINT;
			}
			//如果当前item是小数
		} else {
			String input = item.getInput().substring(0,
					item.getInput().length() - 1);
			if ("".equals(input)) {
				mInputList.remove(item);
				mLastInputstatus = INPUT_OPERATOR;
			} else {
				if (input.contains(".")) {
					item.setInput(input);
					mLastInputstatus = INPUT_POINT;
				} else {
					item.setInput(input);
					mLastInputstatus = INPUT_NUMBER;
				}
			}
		}
	}
	//清理屏
	private void clearAllScreen() {

		clearResultScreen();
		clearInputScreen();

	}
	private void clearResultScreen(){
		mShowResultTv.setText("");
	}

	private void clearInputScreen() {
		mShowInputTv.setText(getResources().getString(R.string.zero));
		mLastInputstatus = INPUT_NUMBER;
		mInputList.clear();
		mInputList.add(new InputItem("", InputItem.InputType.INT_TYPE));
	}
	//计算完成
	private void clearScreen(InputItem item) {
		if(mLastInputstatus != ERROR){
			mLastInputstatus = END;
		}
		mInputList.clear();
		mInputList.add(item);
	}

	//currentStatus 当前状态  9  "9" "+"
	void addInputList(int currentStatus,String inputChar){
		switch (currentStatus) {
		case INPUT_NUMBER:
			if(mLastInputstatus == INPUT_NUMBER){
				InputItem item = (InputItem)mInputList.get(mInputList.size()-1);
				item.setInput(item.getInput()+inputChar);
				item.setType(InputItem.InputType.INT_TYPE);
				mLastInputstatus = INPUT_NUMBER;
			}else if(mLastInputstatus == INPUT_OPERATOR){
				InputItem item = new InputItem(inputChar, InputItem.InputType.INT_TYPE);
				mInputList.add(item);
				mLastInputstatus = INPUT_NUMBER;
			}else if(mLastInputstatus == INPUT_POINT){
				InputItem item = (InputItem)mInputList.get(mInputList.size()-1);
				item.setInput(item.getInput()+inputChar);
				item.setType(InputItem.InputType.DOUBLE_TYPE);
				mLastInputstatus = INPUT_POINT;
			}
			break;
		case INPUT_OPERATOR:
				InputItem item = new InputItem(inputChar, InputItem.InputType.OPERATOR_TYPE);
				mInputList.add(item);
				mLastInputstatus = INPUT_OPERATOR;
			break;
		case INPUT_POINT://point
			 if(mLastInputstatus == INPUT_OPERATOR){
				 InputItem item1 =  new InputItem("0"+inputChar,InputItem.InputType.DOUBLE_TYPE);
				 mInputList.add(item1);
				 mLastInputstatus = INPUT_POINT;
			}else{
				InputItem item1 = (InputItem)mInputList.get(mInputList.size()-1);
				item1.setInput(item1.getInput()+inputChar);
				item1.setType(InputItem.InputType.DOUBLE_TYPE);
				mLastInputstatus = INPUT_POINT;
			}
			break;
		}
	}
}
