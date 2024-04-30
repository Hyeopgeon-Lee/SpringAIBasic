package kopo.poly.service.impl;

import kopo.poly.dto.StudentDTO;
import kopo.poly.mapper.IStudentMapper;
import kopo.poly.service.IStudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class StudentService implements IStudentService {

    private final IStudentMapper studentMapper; // 오라클 DB와 연결된 Mapper

    @Override
    public List<StudentDTO> insertStudent(StudentDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".insertStudent Start!");

        // Student 테이블에 등록된 학생 아이디가 존재하는지 체크하기 위해 DB 조회하기
        Optional<StudentDTO> res = Optional.ofNullable(
                studentMapper.getStudent(pDTO)
        );

        if (res.isEmpty()) { // DB 조회 결과로 회원아이디가 존재하지 않는다면...
            studentMapper.insertStudent(pDTO); // 학생 등록 SQL 실행하기

        }

        // 학생 테이블 전체 조회하기
        List<StudentDTO> rList = Optional.ofNullable(studentMapper.getStudentList()).orElseGet(ArrayList::new);

        log.info(this.getClass().getName() + ".insertStudent End!");

        return rList;
    }

    @Override
    public List<StudentDTO> updateStudent(StudentDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".updateStudent Start!");

        // Student 테이블에 등록된 학생 아이디가 존재하는지 체크하기 위해 DB 조회하기
        Optional<StudentDTO> res = Optional.ofNullable(studentMapper.getStudent(pDTO));

        if (res.isPresent()) { // DB 조회 결과로 회원아이디가 존재한다면...
            studentMapper.updateStudent(pDTO); // 학생 수정 SQL 실행하기
            log.info(pDTO.getUserId() + "님이 수정되었습니다.");

        } else {
            log.info("회원이 존재하지 않아 수정되지 못했습니다.");

        }

        // 학생 테이블 전체 조회하기
        List<StudentDTO> rList = Optional.ofNullable(studentMapper.getStudentList()).orElseGet(ArrayList::new);

        log.info(this.getClass().getName() + ".updateStudent End!");

        return rList;
    }

    @Override
    public List<StudentDTO> deleteStudent(StudentDTO pDTO) throws Exception {

        log.info(this.getClass().getName() + ".deleteStudent Start!");

        // Student 테이블에 등록된 학생 아이디가 존재하는지 체크하기 위해 DB 조회하기
        Optional<StudentDTO> res = Optional.ofNullable(studentMapper.getStudent(pDTO));

        if (res.isPresent()) { // DB 조회 결과로 회원아이디가 존재한다면...
            studentMapper.deleteStudent(pDTO); // 학생 삭제 SQL 실행하기
            log.info(pDTO.getUserId() + "님이 삭제되었습니다.");

        } else {
            log.info("회원이 존재하지 않아 삭제되지 못했습니다.");

        }


        // 학생 테이블 전체 조회하기
        List<StudentDTO> rList = Optional.ofNullable(studentMapper.getStudentList()).orElseGet(ArrayList::new);

        log.info(this.getClass().getName() + ".deleteStudent End!");

        return rList;
    }
}
