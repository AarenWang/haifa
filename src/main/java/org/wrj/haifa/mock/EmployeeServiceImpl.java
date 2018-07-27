package org.wrj.haifa.mock;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService{


    @Override
    public Integer getAllEmployeeCount() {
        return null;
    }

    @Override
    public List<Employee> getEmployeeByName(String fullName) {
        return null;
    }
}
