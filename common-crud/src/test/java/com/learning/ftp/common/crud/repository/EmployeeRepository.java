package com.learning.ftp.common.crud.repository;

import com.learning.ftp.common.crud.entity.Employee;
import static com.learning.ftp.common.crud.entity.Employee.Fields.phones;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends IBaseRepository<Employee, UUID> {

  @Query("select distinct e from Employee e left join fetch e.phones p where p.number in :numbers")
  List<Employee> getEmployeesWithPhone(Collection<String> numbers);

  @EntityGraph(attributePaths = phones)
  @Query("select distinct e from Employee e left join e.phones p where p.number in :numbers")
  List<Employee> getEmployeesWithPhoneGraph(Collection<String> numbers);
}
