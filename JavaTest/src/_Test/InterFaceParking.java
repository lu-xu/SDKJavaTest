package _Test;

import java.math.BigDecimal;

import javax.swing.DefaultComboBoxModel;

import _Test.bean.CarInEntity;
import _Test.bean.CarOutEntity;
import net.sf.json.JSONObject;
import sdk.InterfacePark;
import sdk.UploadUtil;
import sqlite.SqliteJDBC;
import util.FileUtil;

public class InterFaceParking extends InterfacePark {
	private String parkid = MainSDK.PARK_ID;

	@Override
	public void receive(String msg) {
		// TODO Auto-generated method stub
		System.out.println(System.currentTimeMillis() + ">>>>>回调接收: " + msg);
		JSONObject object = JSONObject.fromObject(msg);
		String service_name = object.getString("service_name");
		// MainSDK.textField_4.setText(System.currentTimeMillis()+"");
		String back;
		String errmsg;
		int state = 0;
		boolean querry = false;
		String orderid;
		double derate_money = 0;
		String trade_no;
		int operate_type;

		CarInEntity entity = null;
		JSONObject dataobj = null;
		switch (service_name) {
		case "outpark":
			// {"service_name":"outpark","pay_type":"sweepcode","state":1,"errmsg":"支付成功","trade_no":"kftweixinprepay20010320170725111512897",
			// "order_id":"A1_2C1500952427"}
			orderid = object.getString("order_id");
			trade_no = object.getString("trade_no");
			entity = null;
			for (int i = 0; i < MainSDK.carInList.size(); i++) {
				if (MainSDK.carInList.get(i).getOrder_id().equals(orderid)) {
					querry = true;
					entity = MainSDK.carInList.get(i);
					break;
				} else {
					querry = false;
				}
			}
			if (querry) {
				CarOutEntity outentity = new CarOutEntity();
				outentity.setPay_type(object.getString("pay_type"));
				outentity.setCar_number(entity.getCar_number());
				outentity.setIn_time(entity.getIn_time());
				outentity.setOut_time(System.currentTimeMillis() / 1000);
				outentity.setCar_type(entity.getCar_type());
				outentity.setC_type(entity.getC_type());
				outentity.setUid(entity.getUid());
				outentity.setOut_uid(entity.getUid());
				outentity.setOrder_id(orderid);
				outentity.setOut_channel_id(entity.getIn_channel_id());
				outentity.setTotal(MainSDK.ed_total_now.getText());
				outentity.setAmount_receivable(MainSDK.ed_total.getText());
				outentity.setReduce_amount(MainSDK.edit_decrate_money.getText());
				outentity.setTicket_id(MainSDK.ticket_id.getText());
				// outentity.setElectronic_prepay("0");
				outentity.setElectronic_pay(MainSDK.ed_total_now.getText());
				outentity.setElectronic_prepay(MainSDK.edit_prepay.getText());
				outentity.setService_name("upload_order");
				outentity.setData_target("cloud");
				outentity.setElectronic_pay(MainSDK.ed_total_now.getText());
				try {
					String outmsg = JSONObject.fromObject(outentity).toString();
					System.out.println(">>>>>outpark给云端返回 :" + outmsg);
					String outstate = UploadUtil.uploadData(outmsg);
					System.out.println(">>>>>outpark给云端返回结果 :" + state);
					if (MainSDK.getState(outstate) == 1) {
						SqliteJDBC.Delete(outentity.getOrder_id());
						// MainSDK.ShowDialog(outentity.getCar_number() +
						// "现金出场成功");
						MainSDK.clearOrder();
					}
				} catch (Exception e) {
					// lblNewLabel.setText(e.getMessage());
				}

				String bolinkstr = "{\"service_name\":\"outpark\",\"data_target\":\"bolink\",\"state\":1,\"errmsg\":\"支付成功\",\"trade_no\":\""
						+ trade_no + "\",\"order_id\":\"" + orderid + "\"}";
				System.out.println(">>>>>outpark给bolink返回 :" + bolinkstr);
				String s = UploadUtil.uploadData(bolinkstr);
				System.out.println(">>>>>outpark给bolink返回结果 :" + s);

			} else {

			}
			break;
		case "prepay_order":

			String prepay = object.getString("prepay");
			int prepay_type = object.getInt("prepay_type");
			orderid = object.getString("order_id");

			for (int i = 0; i < MainSDK.carInList.size(); i++) {
				if (MainSDK.carInList.get(i).getOrder_id().equals(orderid)) {
					querry = true;
					entity = MainSDK.carInList.get(i);
					if (prepay_type == 1) {
						// 1电子预支付
						MainSDK.carInList.get(i).setPrepay(prepay);
					} else {
						// 2现金预支付
						MainSDK.carInList.get(i).setPrepay_cash(prepay);
					}

					break;
				} else {
					querry = false;
				}
			}

			if (querry) {
				errmsg = "success";
				state = 1;
				if (prepay_type == 1) {
					// 1电子预支付
					SqliteJDBC.Update("elc_prepay", prepay, orderid);
					MainSDK.edit_prepay.setText(prepay);
				} else {
					// 2现金预支付
					SqliteJDBC.Update("cash_prepay", prepay, orderid);
					MainSDK.edit_prepaycash.setText(prepay);
				}

				String total = MainSDK.ed_total.getText();
				// 减去优惠券的金额
				total = FileUtil.DoubleCalculate(total, entity.getCoupon(), 1) + "";
				// 减去预付
				double totalnow = FileUtil.DoubleCalculate(total, prepay, 0);

				MainSDK.ed_total_now.setText(totalnow + "");

			} else {
				state = 0;
				errmsg = "未查询到此订单";
			}
			back = "{\"service_name\":\"prepay_order\",\"data_target\":\"bolink\",\"errmsg\":\"" + errmsg
					+ "\",\"state\":" + state + ",\"park_id\":\"" + parkid + "\",\"order_id\":\"" + orderid
					+ "\",\"prepay\":\"" + prepay + "\",\"query_time\":" + System.currentTimeMillis() / 100 + "}";
			UploadData(back);
			break;
		case "query_price":

			String order_id;
			if (msg.contains("order_id")) {
				order_id = object.getString("order_id");
			} else {
				order_id = "";
			}
			// price是返回数据的total 订单金额
			double price = 0;
			long duration = 0;
			// total是返回数据的price 当前价格/实时价格
			double Total = 0;
			double derate_duration = 0;
			for (int i = 0; i < MainSDK.carInList.size(); i++) {
				if (MainSDK.carInList.get(i).getOrder_id().equals(order_id)) {
					querry = true;
					// 计算当前的停车价格
					long intime = MainSDK.carInList.get(i).getIn_time();
					duration = System.currentTimeMillis() / 1000 - intime;
					// price = new
					// BigDecimal(FileUtil.calPrice(duration)).setScale(2,
					// BigDecimal.ROUND_HALF_UP)
					// .doubleValue();
					price = FileUtil.doubleFormat(FileUtil.calPrice(duration));

					Total = price;

					// 先减掉预付
					String prepaystr_elc = SqliteJDBC.Querry("order_id", order_id, "elc_prepay");
					String prepaystr_cash = SqliteJDBC.Querry("order_id", order_id, "cash_prepay");
					Total = FileUtil.DoubleCalculate(Total, prepaystr_elc, 0);
					Total = FileUtil.DoubleCalculate(Total, prepaystr_cash, 0);

					// 再减掉优惠券
					String coupon_type = MainSDK.carInList.get(i).getCoupon_type();
					String coupon = MainSDK.carInList.get(i).getCoupon();
					if (coupon_type != null && !coupon_type.equals("")) {
						if (coupon_type.equals("0")) {
							// 0金额，1时长
							derate_duration = 0;
						} else {
							derate_duration = Double.parseDouble(coupon) * 100 / 60;
							// derate_money = "";
						}
						derate_money = Double.parseDouble(coupon);
					}
					Total = FileUtil.DoubleCalculate(Total, coupon, 0);
					break;
				} else if (MainSDK.selectionIndex >= MainSDK.carInList.size()) {
					querry = false;
					break;
				} else if (MainSDK.selectionIndex < MainSDK.carInList.size() && MainSDK.selectionIndex >= 0
						&& msg.contains("out_channel_id") && object.getString("out_channel_id")
								.equals(MainSDK.carInList.get(MainSDK.selectionIndex).getIn_channel_id())) {
					// 通过通道号直接出场，出场直接付
					CarInEntity entityin = MainSDK.carInList.get(MainSDK.selectionIndex);
					order_id = entityin.getOrder_id();
					querry = true;
					duration = System.currentTimeMillis() / 1000 - entityin.getIn_time();
					price = Double.parseDouble(MainSDK.ed_total.getText());

					Total = price;
					// 先减掉预付
					String prepaystr = MainSDK.edit_prepay.getText();
					String prepaystr_cash = MainSDK.edit_prepaycash.getText();
					Total = FileUtil.DoubleCalculate(Total, prepaystr, 0);
					Total = FileUtil.DoubleCalculate(Total, prepaystr_cash, 0);

					// 再减掉优惠券
					String coupon_type = MainSDK.carInList.get(i).getCoupon_type();
					String coupon = MainSDK.carInList.get(i).getCoupon();
					if (coupon_type != null && !coupon_type.equals("")) {
						if (coupon_type.equals("0")) {
							// 0金额，1时长
							derate_duration = 0;
						} else {
							derate_duration = Double.parseDouble(coupon) * 100 / 60;
						}
						derate_money = Double.parseDouble(coupon);
					}
					Total = FileUtil.DoubleCalculate(Total, coupon, 0);
					break;
				} else {
					querry = false;
				}
			}
			if (querry) {
				errmsg = "success";
				state = 1;
			} else {
				state = 0;
				errmsg = "未查询到此订单";
			}
			// = (prices-Double.parseDouble(derate_money))>0
			String query_order_no = "";
			if (object.toString().contains("query_order_no")) {
				query_order_no = object.getString("query_order_no");
			}
			back = "{\"service_name\":\"query_price\",\"data_target\":\"bolink\",\"errmsg\":\"" + errmsg
					+ "\",\"state\":" + state + ",\"park_id\":\"" + parkid + "\",\"order_id\":\"" + order_id
					+ "\",\"price\":\"" + Total + "\",\"duration\":" + ((duration / 60) >= 1 ? (duration / 60) : 1)
					+ ",\"free_out_time\":" + 100 + ",\"query_time\":" + System.currentTimeMillis() / 1000
					+ ",\"derate_money\":\""
					+ (FileUtil.doubleFormat(derate_money) > price ? price : FileUtil.doubleFormat(derate_money))
					+ "\",\"derate_duration\":\"" + derate_duration + "\",\"total\":\"" + price
					+ "\",\"query_order_no\":\"" + query_order_no + "\"}";
			UploadData(back);
			break;
		case "lock_car":

			int islock = object.getInt("is_locked");
			order_id = object.getString("order_id");
			if (islock == 0) {
				// 解锁
				back = "{\"state\":1,\"service_name\":\"lock_car\",\"errmsg\":\"解锁成功\",\"data_target\":\"cloud\",\"order_id\":\""
						+ order_id + "\",\"is_locked\":0}";
			} else {
				// 锁车
				back = "{\"state\":1,\"service_name\":\"lock_car\",\"errmsg\":\"锁车成功\",\"data_target\":\"cloud\",\"order_id\":\""
						+ order_id + "\",\"is_locked\":1}";
			}

			UploadData(back);
			break;
		case "deliver_ticket":
			// 3.10优惠券信息同步（停车云）
			int ticket_type = object.getInt("ticket_type");// 减免类型：0表示减免停车金额
															// 1表示减免停车时长 2 表示全免劵
			order_id = object.getString("order_id");
			String ticket_id = object.getString("ticket_id");
			int Ticket_unit = 2;// 时长劵单位：1表示分钟2表示小时3表示天
			if (ticket_type == 1) {
				Ticket_unit = object.getInt("ticket_unit");
			}
			int index = 0;
			for (int i = 0; i < MainSDK.carInList.size(); i++) {
				if (MainSDK.carInList.get(i).getOrder_id().equals(order_id)) {
					querry = true;
					index = i;
					break;
				} else {
					querry = false;
				}
			}
			if (querry) {
				// 回传给sdk的优惠券金额
				switch (ticket_type) {
				case 0:
					// 减免停车金额
					derate_money = object.getDouble("money");
					break;
				case 1:
					// 减免停车时长
					switch (Ticket_unit) {
					case 1:
						derate_money = Double.parseDouble(object.getString("duration")) * FileUtil.calPrice(60);
						break;
					case 2:
						// 减免停车时长 小时为单位，计算价格传入3600秒，为1小时对应的金额
						derate_money = Double.parseDouble(object.getString("duration")) * FileUtil.calPrice(3600);
						break;
					case 3:
						derate_money = Double.parseDouble(object.getString("duration")) * FileUtil.calPrice(3600 * 24);
						break;
					}
					break;
				case 2:
					// 全免
					derate_money = 1000000;
					break;
				}

				double decrate = FileUtil.doubleFormat(derate_money);
				// 当前金额再减优惠金额
				double totalnow = FileUtil.DoubleCalculate(MainSDK.ed_total_now.getText(), decrate, 0);
				// 优惠金额那一栏填写
				MainSDK.edit_decrate_money.setText(decrate + "");
				// 使用了优惠券后的当前金额
				MainSDK.ed_total_now.setText(totalnow + "");
				SqliteJDBC.Update("coupon", derate_money + "", order_id);
				SqliteJDBC.Update("coupon_type", ticket_type + "", order_id);
				SqliteJDBC.Update("couponid", object.getString("ticket_id"), order_id);
				MainSDK.carInList.get(index).setCoupon(derate_money + "");
				MainSDK.carInList.get(index).setCoupon_type(ticket_type + "");
				MainSDK.carInList.get(index).setCouponid(object.getString("ticket_id"));
				back = "{\"service_name\":\"deliver_ticket\",\"data_target\":\"cloud\",\"ticket_id\":\"" + ticket_id
						+ "\",\"order_id\":\"" + order_id + "\",\"state\":1,\"errmsg\":\"已减免\"}";
			} else {
				back = "{\"service_name\":\"deliver_ticket\",\"data_target\":\"cloud\",\"ticket_id\":\"" + ticket_id
						+ "\",\"order_id\":\"" + order_id
						+ "\",\"derate_money\":\"0\",\"state\":0,\"errmsg\":\"未找到订单\"}";
			}
			UploadData(back);
			break;

		case "monthcard_pay":
			trade_no = object.getString("trade_no");
			String pay_money = object.getString("pay_money");

			back = "{\"service_name\":\"monthcard_pay\",\"data_target\":\"bolink\",\"state\":1,\"trade_no\":\""
					+ trade_no + "\",\"errmsg\":\"月卡续费成功\",\"park_id\":\"" + parkid + "\",\"pay_money\":\"" + pay_money
					+ "\"}";
			UploadData(back);
			break;
		case "nolicence_in_park":
			String user_uuid = object.getString("car_number");
			String park_id = object.getString("park_id");
			String channel_id = object.getString("channel_id");

			long current = System.currentTimeMillis();

			entity = new CarInEntity();
			entity.setCar_number(user_uuid);
			entity.setIn_time(current / 1000);
			entity.setC_type("普通进场");
			entity.setCar_type("小型车");
			entity.setEmpty_plot(99);
			entity.setIn_channel_id(channel_id);
			entity.setOrder_id(current + "");

			entity.setUid(MainSDK.ed_uid.getText());
			MainSDK.carInList.add(entity);
			SqliteJDBC.Insert(entity);

			String msgcarin = JSONObject.fromObject(entity).toString();

			System.out.println("发送进场：" + msgcarin);
			String statein = UploadUtil.uploadInParkOrder(msgcarin);
			System.out.println("生成进场订单：" + state);
			JSONObject stateinjson = JSONObject.fromObject(statein);
			if (stateinjson.getString("state").equals("1")) {
				if (MainSDK.carInList != null && MainSDK.carInList.size() > 0) {
					String[] arr = new String[MainSDK.carInList.size()];
					for (int i = 0; i < MainSDK.carInList.size(); i++) {
						arr[i] = MainSDK.carInList.get(i).getCar_number();
					}
					MainSDK.jListModel = new DefaultComboBoxModel(arr); // 数据模型
					MainSDK.list_orders.setModel(MainSDK.jListModel);
				}
				back = "{\"service_name\":\"nolicence_in_park\",\"data_target\":\"bolink\",\"state\":1,\"errmsg\":\"生成无牌车订单\",\"car_number\":\""
						+ user_uuid + "\",\"timetemp\":" + System.currentTimeMillis() / 1000 + "}";
			} else {
				back = "{\"service_name\":\"nolicence_in_park\",\"data_target\":\"bolink\",\"state\":0,\"errmsg\":\"生成无牌车订单失败\",\"car_number\":\""
						+ user_uuid + "\",\"timetemp\":" + System.currentTimeMillis() / 1000 + "}";
			}
			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			UploadData(back);
			break;
		// ===============================================================================
		// 云端的下行接口
		// ===============================================================================
		case "price_sync":
			// 3.1价格同步
			dataobj = object.getJSONObject("data");
			operate_type = dataobj.getInt("operate_type");
			state = 1;
			String price_id = dataobj.getString("price_id");

			back = "{\"service_name\":\"price_sync\",\"data_target\":\"cloud\",\"operate_type\":" + operate_type
					+ ",\"state\":" + state + ",\"price_id\":\"" + price_id + "\",\"errmsg\":\"价格同步成功！\"}";
			UploadData(back);
			break;
		case "month_card_sync":
			// 3.2月卡套餐同步
			dataobj = object.getJSONObject("data");
			String package_id = dataobj.getString("package_id");
			state = 1;
			operate_type = dataobj.getInt("operate_type");
			back = "{\"service_name\":\"month_card_sync\",\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"package_id\":\"" + package_id + "\",\"operate_type\":" + operate_type
					+ ",\"errmsg\":\" 月卡套餐同步成功！\"}";
			UploadData(back);
			break;
		case "month_member_sync":
			// 3.3月卡会员同步
			dataobj = object.getJSONObject("data");
			state = 1;
			String card_id = dataobj.getString("card_id");
			operate_type = dataobj.getInt("operate_type");
			back = "{\"service_name\":\"month_member_sync\",\"data_target\":\"cloud\",\"card_id\":\"" + card_id
					+ "\",\"state\":" + state + ",\"operate_type\":" + operate_type + ",\"errmsg\":\"月卡会员同步成功！ \"}";
			UploadData(back);
			break;
		case "collector_sync":
			// 3.4车场收费员信息同步
			dataobj = object.getJSONObject("data");
			state = 1;
			String user_id = dataobj.getString("user_id");
			operate_type = dataobj.getInt("operate_type");
			back = "{\"state\":" + state + ",\"user_id\":\"" + user_id
					+ "\",\"service_name\":\"collector_sync\",\"data_target\":\"cloud\",\"operate_type\":"
					+ operate_type + ",\"errmsg\":\"车场收费员信息同步成功！\"}";
			UploadData(back);
			break;
		case "query_prodprice":
			// 3.8查询月卡价格
			trade_no = object.getString("trade_no");
			back = "{\"service_name\":\"query_prodprice\",\"data_target\":\"cloud\",\"state\":1,\"trade_no\":\""
					+ trade_no + "\",\"errmsg\":\"查询月卡价格成功\",\"price\":\"0.01\"}";
			UploadData(back);
			break;
		case "gate_sync":
			// 3.13通道数据下发
			dataobj = object.getJSONObject("data");
			state = 1;
			channel_id = dataobj.getString("channel_id");
			back = "{\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"service_name\":\"gate_sync\",\"errmsg\":\"通道数据下发成功！\",\"channel_id\":\"" + channel_id + "\"}";
			UploadData(back);
			break;
		case "blackuser_sync":
			// 3.14黑名单下发
			dataobj = object.getJSONObject("data");
			state = 1;
			String black_uuid = dataobj.getString("black_uuid");
			back = "{\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"service_name\":\"blackuser_sync\",\"errmsg\":\"黑名单下发成功！\",\"black_uuid\":\"" + black_uuid
					+ "\"}";
			UploadData(back);
			break;
		case "car_type_sync":
			// 3.15车型数据下发
			dataobj = object.getJSONObject("data");
			state = 1;
			String car_type_id = dataobj.getString("car_type_id");
			back = "{\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"service_name\":\"car_type_sync\",\"errmsg\":\"车型数据下发成功！\",\"car_type_id\":\"" + car_type_id
					+ "\"}";
			UploadData(back);
			break;
		case "month_pay_sync":
			// 3.16月卡续费记录下发
			dataobj = object.getJSONObject("data");
			state = 1;
			trade_no = dataobj.getString("trade_no");
			back = "{\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"service_name\":\"month_pay_sync\",\"errmsg\":\"月卡续费记录下发成功！\",\"trade_no\":\"" + trade_no
					+ "\"}";
			UploadData(back);
			break;
		case "confirm_order_inform":
			// 3.17月卡续费记录下发
			state = 1;
			String event_id = object.getString("event_id");
			back = "{\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"service_name\":\"confirm_order_inform\",\"errmsg\":\"手动匹配订单成功！\",\"event_id\":\"" + event_id
					+ "\"}";
			UploadData(back);
			break;
		case "operate_liftrod":
			// 3.18抬杆/落杆通知
			state = 1;
			channel_id = object.getString("channel_id");
			// String channel_name = object.getString("channel_name");
			String operate = object.getString("operate");
			back = "{\"data_target\":\"cloud\",\"state\":" + state
					+ ",\"service_name\":\"operate_liftrod\",\"errmsg\":\"抬杆、落杆通知成功！\",\"channel_id\":\"" + channel_id
					+ "\",\"operate\":\"" + operate + "\"}";
			UploadData(back);
			break;

		// // ======================================================
		// // API更新用户余额
		// // ======================================================
		// case "update_balance":
		// String car_number = object.getString("car_number");
		// String balance = object.getString("balance");
		//
		// break;
		default:
			back = "{\"state\":1,\"service_name\":\"default_service\",\"errmsg\":\"未处理的msg callback\"}";

			break;
		}

	}

	private void UploadData(String back) {
		System.out.println(">>>>>回调返回 :" + back);
		String s = UploadUtil.uploadData(back);
		System.out.println(">>>>>回调返回 结果:" + s);
	}

}
