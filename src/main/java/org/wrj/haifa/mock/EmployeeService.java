package org.wrj.haifa.mock;

import java.util.List;

public interface EmployeeService {

    Integer getAllEmployeeCount();


    List<Employee> getEmployeeByName(String fullName);
}
