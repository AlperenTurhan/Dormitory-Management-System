package com.example.yurt2.service;

import com.example.yurt2.entity.Address;
import com.example.yurt2.entity.AddressStudentRelation;
import com.example.yurt2.entity.Student;
import com.example.yurt2.exception.StudentNotFoundException;
import com.example.yurt2.request.AddressCreateRequest;
import com.example.yurt2.request.StudentCreateRequest;
import com.example.yurt2.validator.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.relational.core.sql.TrueCondition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {
    StudentEntityService studentEntityService;
    AddressService addressService;
    AddressStudentRelationService addressStudentRelationService;
    private final IdentityNumberValidator identityNumberValidator;
    private final TransactionTemplate transactionTemplate;
    private final PhoneNumberValidator phoneNumberValidator;
    private final NameValidator nameValidator;
    private final SurnameValidator surnameValidator;
    private final SchoolNameValidator schoolNameValidator;
    public StudentService(StudentEntityService studentEntityService, AddressService addressService, PlatformTransactionManager transactionTemplate, AddressStudentRelationService addressStudentRelationService,
                          IdentityNumberValidator identityNumberValidator,PhoneNumberValidator phoneNumberValidator,NameValidator nameValidator,SurnameValidator surnameValidator,
                          SchoolNameValidator schoolNameValidator) {
        this.addressService=addressService;
        this.studentEntityService = studentEntityService;
        this.transactionTemplate = new TransactionTemplate(transactionTemplate);
        this.addressStudentRelationService = addressStudentRelationService;
        this.identityNumberValidator=identityNumberValidator;
        this.phoneNumberValidator=phoneNumberValidator;
        this.nameValidator=nameValidator;
        this.surnameValidator=surnameValidator;
        this.schoolNameValidator=schoolNameValidator;
    }
    public List<Student> getAllStudents(){
        return studentEntityService.getAllStudents();
    }
    public Student getOneStudentById(Long studentId, HttpServletResponse httpServletResponse) throws IOException {
        var student = studentEntityService.getOneStudentById(studentId);
        if ( student != null){
            return  student;
        }
        if ( httpServletResponse != null){
            httpServletResponse.sendError(HttpStatus.BAD_REQUEST.value(), "Student could not found with this id.");
            return null;
        }else{
            throw new StudentNotFoundException("Student can not be found by this id.");
        }
    }

    public Student getOneStudentById(Long studentId){
        return getOneStudentById(studentId);
    }
    public Student createStudent(StudentCreateRequest studentCreateRequest){
        identityNumberValidator.validate(studentCreateRequest);
        phoneNumberValidator.validate(studentCreateRequest);
        nameValidator.validate(studentCreateRequest);
        surnameValidator.validate(studentCreateRequest);
        schoolNameValidator.validate(studentCreateRequest);
        var studentStatus = transactionTemplate.execute(p-> {
            Address address = addressService.createAddress(studentCreateRequest.getAddressCreateRequest());
            studentCreateRequest.setAddressId(address.getId());
            Student student = studentEntityService.createStudent(studentCreateRequest);
            addressStudentRelationService.createAddressStudentRelation(createAnInstanceOfAddressStudentRelation(address,student));
            return student;
        });
        return studentStatus;
    }
    public AddressStudentRelation createAnInstanceOfAddressStudentRelation(Address address,Student student){
        AddressStudentRelation addressStudentRelation = new AddressStudentRelation();
        if (addressStudentRelationService.isThereSuchAnAddressStudentRelationByStudentId(student.getId())){
            makeAddressActiveFalse(student.getAddressId());
            addressStudentRelation.setOldAddressId(student.getAddressId());
        }
        addressStudentRelation.setStudentId(student.getId());
        addressStudentRelation.setCurrentAddressId(address.getId());
        addressStudentRelation.setActive(true);
        return addressStudentRelation;
    }
    public void changeStudentAddress(Long studentId,AddressCreateRequest addressCreateRequest){
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Address address = addressService.createAddress(addressCreateRequest);
                Student student = getOneStudentById(studentId);
                addressStudentRelationService.createAddressStudentRelation(createAnInstanceOfAddressStudentRelation(address,student));
                student.setAddressId(address.getId());
            }
        });

    }
    public void makeAddressActiveFalse(Long addressId){
        AddressStudentRelation addressStudentRelation = addressStudentRelationService.getOneAddressStudentRelationByCurrentAddressId(addressId);
        addressStudentRelation.setActive(false);
    }
    public Optional<Student> getOneStudentByIdentityNumber(String identityNumber){
        return studentEntityService.getOneStudentByIdentityNumber(identityNumber);
    }
    public Student updateStudent(Long studentId, Student newStudent){
        return studentEntityService.updateStudent(studentId,newStudent);
    }

    public List<Student> getStudentByName(String name) {
        return studentEntityService.getStudentByName(name);
    }

    public List<String> getSchoolListWithStudentCount() {
        return studentEntityService.getSchoolListWithStudentCount();
    }

    public List<Student> getStudentByNameAndSurname(String name, String surname) {
        return studentEntityService.getStudentByNameAndSurname(name,surname);
    }

    public List<Student> getStudentBySchoolName(String schoolName) {
        return studentEntityService.getStudentBySchoolName(schoolName);
    }
}
