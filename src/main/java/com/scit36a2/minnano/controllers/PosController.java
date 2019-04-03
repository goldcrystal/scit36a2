package com.scit36a2.minnano.controllers;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scit36a2.minnano.dao.PosRepo;
import com.scit36a2.minnano.vo.Menu;
import com.scit36a2.minnano.vo.Payment;
import com.scit36a2.minnano.vo.Sales_detail;
import com.scit36a2.minnano.vo.Sales_state;
import com.scit36a2.minnano.vo.Seat;

// POS기능 - 메인화면에서 일어나는 기능들 
// 영업개시(시재관리), 테이블 조회, 판매전표 조회, 
// 테이블 이동(=판매조회 전표에 저장된 데이터 수정), 영업마감...
// POS 기능 - 주문화면에서 일어나는 기능들
// 테이블 번호와 
// 
@Controller
public class PosController {

	@Autowired
	PosRepo repo;

	private static final Logger logger = LoggerFactory.getLogger(PosController.class);

	@RequestMapping(value = "/pos", method = RequestMethod.GET)
	public String pos() {
		logger.info("welcome pos.");
		return "pos/pos";
	}

	// req list for Order-in-progress
	@RequestMapping(value = "/seatsavailable", method = RequestMethod.POST)
	public @ResponseBody String show(HttpSession session) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		ArrayList<HashMap<String, Object>> seats = repo.seatsavailable(comp_seq);
		ObjectMapper objmap = new ObjectMapper();
		String result = "";
		try {
			result = objmap.writeValueAsString(seats);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		System.out.println(result);
		return result;
	}

	// create order 임시방편으로
	@RequestMapping(value = "makeorder", method = RequestMethod.POST)
	public @ResponseBody String makeorder(HttpSession session, Sales_state sas, Sales_detail sad, String ppod) {
//		System.out.println(sas + ", " + sad + ", " + ppod);
		String result = "";
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		int seqno = repo.chksasseqs();
		sas.setSales_state_seq(seqno);
		sas.setComp_seq(comp_seq);
		int resultSas = repo.insertSas(sas);
		int resultSad = 0;
		String ppods[] = ppod.split("\\|");
		int chker = 0;
		for (int i = 0; i < ppods.length - 3; i += 4) {
			chker++;
			if (ppods[i].equals("-1")) {
				sad.setSales_discount(Integer.parseInt(ppods[i + 2]));
				sad.setMenu_seq(-1);
			} else {
				sad.setMenu_seq(Integer.parseInt(ppods[i]));
				sad.setSales_order(Integer.parseInt(ppods[i + 3]));
			}
			sad.setSales_state_seq(seqno);
			resultSad += repo.insertSad(sad);
		}
		if (resultSas == 1 && resultSad == chker) {
			result = "success";
		} else {
			result = "fail";
		}
		return result;
	}

	// create order 임시방편으로
	@RequestMapping(value = "replaceorder", method = RequestMethod.POST)
	public @ResponseBody String replaceorder(HttpSession session, Sales_state sas, Sales_detail sad, String ppod) {
		String result = "";
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		int sas_seq = sas.getSales_state_seq();
		sas.setComp_seq(comp_seq);
		sas.setSales_state_seq(sas_seq);
		int deleteOld = repo.deleteoldorder(sas_seq);

		String ppods[] = ppod.split("\\|");
		int newodsize = ppod.length();

		int resultreplaceorder = 0;
		int chker = 0;
		for (int i = 0; i < ppods.length - 3; i += 4) {
			chker++;
			if (ppods[i].equals("-1")) {
				sad.setSales_discount(Integer.parseInt(ppods[i + 2]));
				sad.setMenu_seq(-1);
			} else {
				sad.setMenu_seq(Integer.parseInt(ppods[i]));
				sad.setSales_order(Integer.parseInt(ppods[i + 3]));
			}
			sad.setSales_state_seq(sas_seq);
			resultreplaceorder += repo.insertSad(sad);
		}

		if (resultreplaceorder == chker) {
			result = "success";
		} else {
			result = "fail";
		}
		return result;
	}

	@RequestMapping(value = "alOrderList", method = RequestMethod.POST)
	public @ResponseBody ArrayList<Sales_detail> alOrderList(HttpSession session, int sas_seq) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		ArrayList<Sales_detail> sadList = repo.alOrderList(sas_seq);

		return sadList;
	}

	@RequestMapping(value = "makepayment", method = RequestMethod.POST)
	public @ResponseBody String makepayment(HttpSession session, Payment pmt) {
		String result = "";
		String emp_id = (String) session.getAttribute("emp_id");
		pmt.setPayment_clerk(emp_id);

		int sas_seq = pmt.getSales_state_seq();
		int sasupdateresult = repo.updatesasdone(sas_seq);
		int makepmtresult = repo.makepayment(pmt);

		return result;
	}

	@RequestMapping(value = "makepaymentcomplex", method = RequestMethod.POST)
	public @ResponseBody String makepaymentcomplex(HttpSession session, String pmtcmp, Payment pmt) {
		String result = "";
		String emp_id = (String) session.getAttribute("emp_id");
		System.out.println(pmtcmp);
		String pmtcmps[] = pmtcmp.split("\\|");
		int sas_seq = Integer.parseInt(pmtcmps[0]);
		int chker = 0;

		int sasupdateresult = repo.updatesasdone(sas_seq);
		
		pmt.setSales_state_seq(sas_seq);
		pmt.setPayment_clerk(emp_id);
		
		int makepmtresult = 0;
		for (int i = 1; i < pmtcmps.length-1; i += 2) {
			chker++;
			pmt.setPayment_type(Integer.parseInt(pmtcmps[i]));
			pmt.setPayment_amount(Integer.parseInt(pmtcmps[i+1]));
			makepmtresult += repo.makepayment(pmt);
			
			pmt.setPayment_type(0);
			pmt.setPayment_type(0);
		}

		return result;
	}

	@RequestMapping(value = "selectPOSone", method = RequestMethod.POST)
	@ResponseBody
	public ArrayList<HashMap<String, Object>> selectPOSone(HttpSession session, Seat seat, Sales_state sales_state,
			Sales_detail sales_detail) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");

		ArrayList<HashMap<String, Object>> result = repo.selectPOSone(comp_seq);

		System.out.println("selectPOSone1 result : " + result);

		return result;
	}

	@RequestMapping(value = "selectPOStwo", method = RequestMethod.POST)
	@ResponseBody
	public ArrayList<HashMap<String, Object>> selectPOStwo(HttpSession session, Menu menu, Sales_state sales_state,
			Sales_detail sales_detail) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		ArrayList<HashMap<String, Object>> result = repo.selectPOStwo(comp_seq);
		System.out.println("selectPOStwo2 result" + result);
		return result;
	}

	@RequestMapping(value = "deleteSasSadPay", method = RequestMethod.POST)
	@ResponseBody
	public String deleteSasSadPay(Sales_state sales_state, HttpSession session) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		System.out.println("삭제 컨트롤러 comp_seq : " + comp_seq);
		sales_state.setComp_seq(comp_seq);

		System.out.println("삭제 컨트롤러 sales_state : " + comp_seq);
		int result = repo.deleteSasSadPay(comp_seq);
		System.out.println("삭제 컨트롤러 result : " + result);
		return "success";
	}

	@RequestMapping(value = "updatePOStwo", method = RequestMethod.POST)
	@ResponseBody
	public int updatePOStwo(HttpSession session, int sales_state_seq, Sales_state sales_state) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		sales_state.setComp_seq(comp_seq);
		// sales_state.setSales_state_seq(sales_state_seq);
		System.out.println("sales_state컨트롤러 : " + sales_state);
		int result = repo.updatePOStwo(sales_state);
		System.out.println("result 컨트롤러 : " + result);
		return result;
	}

	//
	//
	//////
	//////
	//
	// will be deleted soon below

	// create sales_state and sub-sales_details
	@RequestMapping(value = "insertSasSad", method = RequestMethod.POST)
	@ResponseBody
	public String insertSasSad(HttpSession session, Sales_state sales_state, Sales_detail sales_detail, Menu menu) {
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		System.out.println("comp_seq" + comp_seq);

		// sas
		// seq(자동생성), comp(session), sales_start(sysdate), sales_end(sysdate+1)
		// seat_seq(ajax), sales_visitors(ajax), sales_memo(ajax)
		// sad
		// seq(생성), sas_seq(부여)
		// menu_seq(ajax), sales_order(ajax), sales_discount(ajax)

		menu.setComp_seq(comp_seq);

		System.out.println("menu" + menu);
		//////////////////////////////////
		HashMap<String, Object> map = new HashMap<>();

		map.put("menu", menu);
		map.put("sales_state_seq", sales_state);
		map.put("sales_detail", sales_detail);

		map.put("sales_state_seq", sales_state.getSales_state_seq());
		map.put("comp_seq", sales_state.getComp_seq());
		map.put("seat_seq", sales_state.getSeat_seq());
		map.put("sales_start", sales_state.getSales_start());
		map.put("sales_end", sales_state.getSales_end());
		map.put("sales_visitors", sales_state.getSales_visitors());
		map.put("sales_memo", sales_state.getSales_memo());
		map.put("sales_detail_seq", sales_detail.getSales_detail_seq());
		map.put("sales_state_seq", sales_detail.getSales_state_seq());
		map.put("menu_seq", sales_detail.getMenu_seq());
		map.put("sales_order", sales_detail.getSales_order());
		map.put("sales_discount", sales_detail.getSales_discount());

		int result = repo.insertSasSad(map);
		System.out.println("result1 : " + result);
		if (result != 0) {
			System.out.println("O : " + result);
		} else {
			System.out.println("X : " + result);
		}
		System.out.println("result3 : " + result);
		return "success";
	}

}
