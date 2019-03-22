package com.scit36a2.minnano.dao;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.scit36a2.minnano.vo.Company;
import com.scit36a2.minnano.vo.Employee;


@Repository
public class MemberRepo {
	
	@Autowired
	SqlSession session;

	/*
	 * 회원 아이디로 회원 검색
	 * 
	 */
	public Employee selectOne(Employee employee) {
		MemberDAO dao = session.getMapper(MemberDAO.class);
		Employee e = dao.selectOne(employee);
		return e;
	}
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	/*
	 * 알바생 등록 리스트 현황 불러오기
	 */
	public List<Employee> employeeList() {
		MemberDAO dao = session.getMapper(MemberDAO.class);
		List<Employee> employeeList = dao.selectAll(null);
		return employeeList;
		
	}
	/**
	 * 사장님 정보 수정 요청 처리
	 * @param employee
	 * @return
	 */
	public int updateOwner(Employee employee) {
		MemberDAO dao = session.getMapper(MemberDAO.class);
		int result = dao.updateOwner(employee);
		return result;
		
	}
	public int join(HashMap<String, Object> map) {
		MemberDAO dao = session.getMapper(MemberDAO.class);
		int result = dao.join(map);
		return result;
	}
	public int reqCompSeq() {
		MemberDAO dao = session.getMapper(MemberDAO.class);
		int result = dao.reqCompSeq();
		return result;
	}
	
	/*
	 * 사업자 등록번호 중복체크
	 */
	
	public Company selectCompanyOne(Company company) {
		MemberDAO dao = session.getMapper(MemberDAO.class);
		Company c = dao.selectCompanyOne(company);
		return c;
	}
	

}
