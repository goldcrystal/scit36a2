package com.scit36a2.minnano.controllers;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.scit36a2.minnano.dao.BoardRepo;
import com.scit36a2.minnano.dao.MemberRepo;
import com.scit36a2.minnano.util.FileService;
import com.scit36a2.minnano.util.PageNavigator;
import com.scit36a2.minnano.vo.Board;
import com.scit36a2.minnano.vo.Board_comments;
import com.scit36a2.minnano.vo.Employee;

// 커뮤니티 기능
@Controller
public class BoardController {
	private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

	@Autowired
	BoardRepo repo;
	@Autowired
	MemberRepo membrepo;

	// 파일이 저장될 경로
	final String uploadPath = "/boardfile";

	/**
	 * 글목록 보기 및 글목록 내에서 이동 시 paging 처리와 search 처리
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "/board", method = RequestMethod.GET)
	public String boardList(@RequestParam(value = "searchItem", defaultValue = "title") String searchItem,
			@RequestParam(value = "searchWord", defaultValue = "") String searchWord,
			@RequestParam(value = "currentPage", defaultValue = "1") int currentPage, Model model,
			HttpSession session) {

		// DB 접속 코드
		int totalRecoundCount = repo.totalBoardCount(searchItem, searchWord); // search한것의 전체 게시글 수
		PageNavigator navi = new PageNavigator(currentPage, totalRecoundCount);
		Employee employee = new Employee();
		List<HashMap<String, Object>> boardList = repo.boardList(searchItem, searchWord, navi.getStartRecord(),
				navi.getCountPerPage());

		// String emp_id = (String)session.getAttribute("emp_id");
		// emp_seq를 repo보내서 member매퍼에서 select emp_id 또는 emp_name 또는 comp_seq로 comp_name을
		// 불러와서 넣어줄수도있죠...
		model.addAttribute("boardList", boardList);
		model.addAttribute("navi", navi);
		model.addAttribute("searchWord", searchWord);
		model.addAttribute("searchItem", searchItem);
		// model.addAttribute("emp_id",emp_id);
		return "board/board";
	}

	// 화면이동: 글쓰기
	@RequestMapping(value = "/boardRegist", method = RequestMethod.GET)
	public String boardRegist() {
		return "board/boardRegist";
	}

	/**
	 * 게시글쓰기 처리
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "boardRegist", method = RequestMethod.POST)
	@ResponseBody
	public int insertBoard(Board board, HttpSession session) {
		int emp_seq = (Integer) session.getAttribute("emp_seq");
		board.setEmp_seq(emp_seq);
		if ( board.getBoard_orgname() == null) {
			board.setBoard_orgname("");
		}
		board.setBoard_savname("");
		int result = repo.insertBoard(board);
		return result;
	}

	/**
	 * 글 자세히 보기
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "/boardDetail", method = RequestMethod.GET)
	public String boardDetail(@RequestParam(value = "searchItem", defaultValue = "title") String searchItem,
			@RequestParam(value = "searchWord", defaultValue = "") String searchWord,
			@RequestParam(value = "currentPage", defaultValue = "1") int currentPage, int board_seq, Model model) {
		Board board = repo.boardDetail(board_seq);
		String id = repo.getEmpId(board_seq);
//		String mime = null;
//		if (board.getBoard_orgname() != null) {
//			String fullPath = uploadPath + "/" + board.getBoard_savname();
//
//			try {
//				mime = Files.probeContentType(Paths.get(fullPath));
//				if (mime.contains("image"))
//					model.addAttribute("mime", mime);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		int totalRecordCount = repo.totalBoardCount(searchItem, searchWord);
		PageNavigator navi = new PageNavigator(currentPage, totalRecordCount);
		// 검색을 한 후 글 자세히보기로 왔을 때를 처리하기 위해
		model.addAttribute("board", board);
		model.addAttribute("navi", navi);
		model.addAttribute("searchWord", searchWord);
		model.addAttribute("searchItem", searchItem);
		model.addAttribute("id", id);
		return "board/boardDetail";
	}

	/**
	 * 리플 호출
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "selectComment", method = RequestMethod.POST)
	@ResponseBody
	public List<Board_comments> selectComment(Board_comments board_comments, int board_seq) {
		board_comments.setBoard_seq(board_seq);
		List<Board_comments> result = repo.selectComment(board_comments);
		return result;
	}

	/**
	 * 글 삭제
	 * 
	 * @author cck, lyc
	 */

	@RequestMapping(value = "/boardDelete", method = RequestMethod.GET)
	public String boardDelete(int board_seq, HttpSession session, Board board) {
		Board oldBoard = repo.selectOne(board_seq);
		int emp_seq = (Integer) session.getAttribute("emp_seq");
		board.setEmp_seq(emp_seq);
		String savedfile = oldBoard.getBoard_savname();
		// HDD에 저장된 파일 삭제
		if (savedfile != null) {
			String fullPath = uploadPath + "/" + savedfile;
			boolean result = FileService.deleteFile(fullPath);
		}
		// DB에 저장된 글 삭제
		repo.boardDelete(board);

		return "redirect:/board";
	}

	// 수정페이지 이동
	@RequestMapping(value = "/boardUpdate", method = RequestMethod.GET)
	public String boardUpdate(int board_seq, HttpSession session, Model model) {
		Board board = repo.selectOne(board_seq);
		model.addAttribute("board", board);
		return "board/boardUpdate";
	}

	/**
	 * 게시글 수정처리
	 * 
	 * 여기 oldBoard 사용 안하고 있는데 확인해보시기 바랍니다.
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "/boardUpdate", method = RequestMethod.POST)
	public String boardUpdate(Board board, MultipartFile upload, HttpSession session) {
		// 파일이 수정된 파일(), 원래파일, 원래부터 파일이 없던 경우
		int emp_seq = (int) session.getAttribute("emp_seq");
		board.setEmp_seq(emp_seq);
		Board oldBoard = repo.selectOne(board.getBoard_seq());
		// System.out.println("수정 board : " + board);
		repo.boardUpdate(board);
		return "redirect:/board";
	}

	/**
	 * 리플달기
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "inputComment", method = RequestMethod.POST)
	@ResponseBody
	public String inputComment(Board_comments board_comments, HttpSession session, String board_comments_writer,
			String board_comments_content) {
		// board_comments.setBoard_comments_writer(board_comments_writer);
		String emp_id = (String) session.getAttribute("emp_id");
		board_comments.setBoard_comments_content(board_comments_content);

		Employee writer = new Employee();
		writer.setEmp_id(emp_id);
		writer = membrepo.selectOne(writer);
		board_comments.setBoard_comments_writer(writer.getEmp_name());
		// System.out.println("board_comments : " + board_comments);
		int result = repo.inputComment(board_comments);

		return "success";
	}

	/**
	 * 리플삭제
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "deleteComment", method = RequestMethod.POST)
	@ResponseBody
	public String deleteComment(Board_comments board_comments, HttpSession session, Employee writer) {
		int emp_seq = (Integer) session.getAttribute("emp_seq");
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		writer.setComp_seq(comp_seq);
		writer.setEmp_seq(emp_seq);
		writer = membrepo.selectEmployee(writer);
		board_comments = repo.selectCmtOne(board_comments);
		
		if ( writer.getEmp_name().equals(board_comments.getBoard_comments_writer()) ) {
			System.out.println("조건에 맞으면 삭제합니다." + board_comments);
			int result = repo.deleteComment(board_comments);
			if ( result == 1 ) {
				return "success";
			}
		} 
		return "failure";
	}

	/**
	 * 리플수정
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "updateComment", method = RequestMethod.POST)
	@ResponseBody
	public String updateComment(HttpSession session, Board_comments board_comments, Employee writer) {
		int emp_seq = (Integer) session.getAttribute("emp_seq");
		int comp_seq = (Integer) session.getAttribute("comp_seq");
		writer.setComp_seq(comp_seq);
		writer.setEmp_seq(emp_seq);
		writer = membrepo.selectEmployee(writer);
		board_comments = repo.selectCmtOne(board_comments);
		
		if ( writer.getEmp_name().equals(board_comments.getBoard_comments_writer()) ) {
			System.out.println("조건에 맞으면 수정합니다." + board_comments);
			int result = repo.updateComment(board_comments);
			if ( result == 1 ) {
				return "success";
			}
		} 
		return "failure";
	}
	
	/**
	 * 통계보기 연결해주기
	 * 
	 * @author cck, lyc
	 */
	@RequestMapping(value = "showreport", method = RequestMethod.GET)
	public String showreport(HttpSession session, Board board) {
		return "report/synthesize";
	}

	
}
