package util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import _Test.bean.AnalysEntity;

public class FileUtil {
	public static double calPrice(long duration) {
		// ��λ����
//		 return duration*0.01/900+0.01;
		 return 0.01;
//		return duration * 0.01 / 60 + 1.01;
//		return 5;
	}

	public static String AnalysLog(String content, List<String> target) {
		List<Integer> times = new ArrayList<>();
		for (String s : target) {
			String[] temp = s.split(content);
			times.add(Integer.parseInt(temp[1]));

		}
		int ms50 = 0, ms200 = 0, ms1000 = 0, ms1000_ = 0;
		int total = 0;
		for (int i : times) {
			if (i < 50) {
				ms50++;
			} else if (i < 200) {
				ms200++;
			} else if (i < 1000) {
				ms1000_++;
			} else {
				ms1000++;
			}
			total += i;
		}
		double average = total / target.size();
		double ms50p = ms50 / target.size() * 100;
		double ms200p = ms200 / target.size() * 100;
		double ms1000p = ms1000 / target.size() * 100;
		double ms1000p_ = ms1000_ / target.size() * 100;
		AnalysEntity entity = new AnalysEntity();
		entity.setMs50(ms50);
		entity.setMs200(ms200);
		entity.setMs1000(ms1000);
		entity.setMs50p(ms50p);
		entity.setMs200p(ms200p);
		entity.setMs1000p(ms1000p);
		entity.setAverage(average);
		String msg = content + target.size() + "��\n 50ms����" + ms50 + "��������" + ms50p + "%\n 50~200ms" + ms200
				+ "��������" + ms200p + "%\n 200~1000s" + ms1000_ + "��������" + ms1000p_ + "%\n 1s����" + ms1000
				+ "��������" + ms1000p + "%\n ƽ��" + average + "ms\n";
		entity.setMsg(msg);
		return msg;
	}
	
	/**
	 * 计算任意两个值的数学结果
	 * @param target
	 * @param target2
	 * @param type 1代表相加，其余为减
	 * @return 保留两位小数的double值
	 */
	public static <T> double DoubleCalculate(T target, T target2, int type) {
		double result, tar, tar2;

		tar = str2double(target);
		tar2 = str2double(target2);

		if (type == 1) {
			result = new BigDecimal(tar + tar2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		} else {
			result = new BigDecimal(tar - tar2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		}
		if (result < 0)
			result = 0;
		return result;
	}

	public static <T> double str2double(T target) {
		double tar;
		if (target != null && !target.equals(""))
			tar = Double.parseDouble(target.toString());
		else
			tar = 0;
		return tar;
	}
	
	public static double doubleFormat(double target){
		return new BigDecimal(target).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
}
