package org.client_server.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private long id;

    @NotBlank(message = "Full name không được để trống")
    private String name;

    @NotNull(message = "DOB không được để trống")
    private LocalDate dob;

    //inclusive = true thì có thể bằng, false thì không chấp nhận bằng
    @DecimalMax(value ="4.0", inclusive = true, message = "GPA phải >= 4.0")
    @DecimalMin(value ="0.0", inclusive = true, message = "GPA phải >= 0.0")
    private double gpa;

    @NotNull(message = "Sex không được để trống")
    private Sex sex;

    @NotBlank(message = "Major không được để trống")
    private String major;

}
