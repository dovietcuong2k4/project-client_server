package org.client_server.model;

import java.time.LocalDate;

public class Student {
    private String name;
    private String dob;
    private String studentId;
    private double gpa;
    private String sex;
    private String major;

    public Student(String name, String dob, String studentId, double gpa, String sex, String major) {
        this.name = name;
        this.dob = dob;
        this.studentId = studentId;
        this.gpa = gpa;
        this.sex = sex;
        this.major = major;
    }

    public Student() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }
}
