package com.learning.ftp.common.crud;

import com.learning.ftp.common.crud.dto.EmployeeWithIdOnly;
import com.learning.ftp.common.crud.entity.Employee;
import static com.learning.ftp.common.crud.entity.Employee.Fields.birthday;
import static com.learning.ftp.common.crud.entity.Employee.Fields.firstName;
import static com.learning.ftp.common.crud.entity.Employee.Fields.id;
import static com.learning.ftp.common.crud.entity.Employee.Fields.lastName;
import static com.learning.ftp.common.crud.entity.Employee.Fields.phones;
import static com.learning.ftp.common.crud.entity.Employee.Fields.salary;
import com.learning.ftp.common.crud.entity.Phone;
import com.learning.ftp.common.crud.entity.Phone.Fields;
import static com.learning.ftp.common.crud.entity.Phone.Fields.number;
import static com.learning.ftp.common.crud.entity.Phone.Fields.owner;
import com.learning.ftp.common.crud.repository.EmployeeRepository;
import com.learning.ftp.common.crud.repository.PhoneRepository;
import com.learning.ftp.common.crud.repository.RepoHelper;
import com.learning.ftp.common.crud.repository.RepoHelper.QueryDto;
import com.learning.ftp.common.crud.repository.specification.FbSpec;
import com.learning.ftp.common.crud.repository.specification.FbSpec.FbCustomSpec;
import com.learning.ftp.common.crud.repository.specification.FbSpec.FbSpecBuilder;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.between;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.builder;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.endWith;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.eq;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.fetch;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.ge;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.gt;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.join;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.le;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.like;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.lt;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.notEqual;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.startWith;
import static com.learning.ftp.common.crud.repository.specification.FbSpec.subquery;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import static javax.persistence.criteria.JoinType.LEFT;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.IsNot.not;
import static org.hibernate.Hibernate.initialize;
import org.hibernate.LazyInitializationException;
import org.hibernate.transform.PassThroughResultTransformer;
import org.hibernate.type.PostgresUUIDType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import static org.springframework.data.jpa.domain.Specification.where;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
@Slf4j
@SpringBootTest(classes = TestApplication.class)
@TestExecutionListeners(value = SpecIntegrationTest.class, mergeMode = MERGE_WITH_DEFAULTS)
class SpecIntegrationTest implements TestExecutionListener {
  @Autowired private EmployeeRepository employeeRepository;
  @Autowired private PhoneRepository phoneRepository;
  @Autowired private RepoHelper helper;

  @Autowired
  @Qualifier("findEmployeesByPhones")
  String findEmployeesByPhones;

  @Autowired
  @Qualifier("findEmployeesIdByPhones")
  String findEmployeesIdByPhones;

  private static Employee employeeNamNguyen;
  private static Employee employeeMinhLe;
  private static Employee employeeHangNguyen;

  @Override
  public void beforeTestClass(TestContext testContext) {
    testContext.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(this);
    phoneRepository.deleteAll();
    employeeRepository.deleteAll();

    employeeNamNguyen =
        createEmployee(
            "Nam",
            "Nguyen",
            20.0,
            ZonedDateTime.of(1990, 7, 11, 0, 0, 0, 0, ZoneId.systemDefault()));
    employeeMinhLe =
        createEmployee(
            "Minh", "Le", 26.0, ZonedDateTime.of(1971, 1, 29, 0, 0, 0, 0, ZoneId.systemDefault()));
    generatePhone(employeeMinhLe, 1, 2);
    employeeHangNguyen =
        createEmployee(
            "Hang",
            "Nguyen",
            32.0,
            ZonedDateTime.of(2015, 9, 9, 0, 0, 0, 0, ZoneId.systemDefault()));
    generatePhone(employeeHangNguyen, 4, 6);
  }

  private Employee createEmployee(
      String firstName, String lastName, Double salary, ZonedDateTime birthday) {
    Employee employee = new Employee();
    employee.setId(UUID.randomUUID());
    employee.setFirstName(firstName);
    employee.setLastName(lastName);
    employee.setSalary(salary);
    employee.setBirthday(birthday);
    return employeeRepository.save(employee);
  }

  private void generatePhone(Employee employee, int start, int end) {
    List<Phone> phones =
        IntStream.range(start, end)
            .mapToObj(String::valueOf)
            .map(
                num -> {
                  var phone = new Phone();
                  phone.setId(UUID.randomUUID());
                  phone.setNumber(num);
                  phone.setOwner(employee);
                  return phone;
                })
            .collect(toList());
    employee.setPhones(phoneRepository.saveAll(phones));
    employeeRepository.save(employee);
  }

  @Test
  public void givenSalary_whenGettingListOfEmployees_thenCorrect() {
    List<Employee> results = employeeRepository.findAll(eq(salary, 32));

    assertThat(results, hasSize(1));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeHangNguyen.getFirstName(), in(firstNames));
  }

  @Test
  public void givenNotEqualSalary_whenGettingListOfEmployees_thenCorrect() {
    List<Employee> results = employeeRepository.findAll(notEqual(salary, 32));

    assertThat(results, hasSize(2));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), in(firstNames));
    assertThat(employeeMinhLe.getFirstName(), in(firstNames));
    assertThat(employeeHangNguyen.getFirstName(), not(in(firstNames)));
  }

  @Test
  public void givenGreaterThanSalary_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> salarySpec = gt(salary, 26);
    List<Employee> results = employeeRepository.findAll(where(salarySpec));

    assertThat(results, hasSize(1));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeHangNguyen.getFirstName(), in(firstNames));
  }

  @Test
  public void givenGreaterThanEqualSalary_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> salarySpec = ge(salary, 26);
    List<Employee> results = employeeRepository.findAll(where(salarySpec));

    assertThat(results, hasSize(2));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), not(in(firstNames)));
    assertThat(employeeMinhLe.getFirstName(), in(firstNames));
    assertThat(employeeHangNguyen.getFirstName(), in(firstNames));
  }

  @Test
  public void givenLessThanSalary_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> salarySpec = lt(salary, 26);
    List<Employee> results = employeeRepository.findAll(where(salarySpec));

    assertThat(results, hasSize(1));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), in(firstNames));
  }

  @Test
  public void givenLessThanEqualSalary_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> salarySpec = le(salary, 26);
    List<Employee> results = employeeRepository.findAll(where(salarySpec));

    assertThat(results, hasSize(2));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), in(firstNames));
    assertThat(employeeMinhLe.getFirstName(), in(firstNames));
    assertThat(employeeHangNguyen.getFirstName(), not(in(firstNames)));
  }

  @Test
  public void givenSalaryBetween_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> salarySpec = between(salary, 20L, 26L);
    List<Employee> results = employeeRepository.findAll(where(salarySpec));

    assertThat(results, hasSize(2));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), in(firstNames));
    assertThat(employeeMinhLe.getFirstName(), in(firstNames));
    assertThat(employeeHangNguyen.getFirstName(), not(in(firstNames)));
  }

  @Test
  public void givenLikeName_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> likeNameSpec = like(firstName, "A");
    List<Employee> results = employeeRepository.findAll(where(likeNameSpec));

    assertThat(results, hasSize(2));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), in(firstNames));
    assertThat(employeeMinhLe.getFirstName(), not(in(firstNames)));
    assertThat(employeeHangNguyen.getFirstName(), in(firstNames));
  }

  @Test
  public void givenNameStartWith_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> nameStartWithSpec = startWith(firstName, "mi");
    List<Employee> results = employeeRepository.findAll(where(nameStartWithSpec));

    assertThat(results, hasSize(1));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeMinhLe.getFirstName(), in(firstNames));
  }

  @Test
  public void givenNameEndWith_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> nameEndWithSpec = endWith(firstName, "nh");
    List<Employee> results = employeeRepository.findAll(where(nameEndWithSpec));

    assertThat(results, hasSize(1));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeMinhLe.getFirstName(), in(firstNames));
  }

  @Test
  public void givenFirstAndLastName_whenGettingListOfEmployees_thenCorrect() {
    Specification<Employee> firstNameSpec = eq(firstName, "Nam");
    Specification<Employee> lastNameSpec = eq(lastName, "Nguyen");
    List<Employee> results = employeeRepository.findAll(where(firstNameSpec).and(lastNameSpec));

    assertThat(results, hasSize(1));
    assertThat(
        "Firstname is Nam", results.get(0).getFirstName().equals(employeeNamNguyen.getFirstName()));
    assertThat(
        "Lastname is Nguyen", results.get(0).getLastName().equals(employeeNamNguyen.getLastName()));
  }

  @Test
  public void givenFirstOrLastName_whenGettingListOfEmployees_thenCorrect() {
    FbSpecBuilder<Employee> conditions = builder();
    FbCustomSpec<Employee> firstNameCond = eq(firstName, "Nam");
    conditions.spec(firstNameCond.or(eq(lastName, "Le")));
    List<Employee> results = employeeRepository.findAll(conditions.build());

    assertThat(results, hasSize(2));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeNamNguyen.getFirstName(), in(firstNames));
    List<String> lastNames = results.stream().map(Employee::getLastName).collect(toList());
    assertThat(employeeMinhLe.getLastName(), in(lastNames));
  }

  @Test
  public void givenNameLikeAndGreaterThanSalary_whenGettingListOfEmployees_thenCorrect() {
    FbSpecBuilder<Employee> conditions = FbSpec.builder();
    conditions.spec(like(firstName, "a"));
    conditions.spec(gt(salary, 20));
    List<Employee> results = employeeRepository.findAll(conditions.build());
    assertThat(results, hasSize(1));
    List<String> firstNames = results.stream().map(Employee::getFirstName).collect(toList());
    assertThat(employeeHangNguyen.getFirstName(), in(firstNames));
  }

  @Test
  @Transactional
  public void givenPhone_whenGettingListOfEmployees_thenCorrectWithLazyLoadInTransaction() {
    String phone = "4";
    Specification<Employee> specification =
        (root, query, builder) -> builder.equal(root.join(phones).get(number), phone);
    List<Employee> results = employeeRepository.findAll(specification);
    assertThat(results, hasSize(1));
    assertThat(results.get(0).getPhones(), hasItem(hasProperty(number, equalTo(phone))));
  }

  @Test
  public void whenGettingListOfEmployees_thenCorrectWithJoinFetch() {
    String name = employeeHangNguyen.getFirstName();
    Specification<Employee> fetch = fetch(phones);
    Specification<Employee> condition = eq(firstName, name);
    List<Employee> results = employeeRepository.findAll(fetch.and(condition));
    assertThat(results, hasSize(1));
    Employee employee = results.get(0);
    assertThat(employee.getFirstName(), equalTo(name));
    assertThat(employee.getPhones(), hasSize(2));
    assertThat(employee.getPhones(), hasItem(hasProperty(number, equalTo("4"))));
  }

  @Test
  public void
      whenGettingListOfEmployeesWithFetchAndCondition_thenCorrectWithOnlyMatchedRowsFetched() {
    String name = employeeHangNguyen.getFirstName();
    String phone = employeeHangNguyen.getPhones().get(0).getNumber();
    String notContainPhone = employeeHangNguyen.getPhones().get(1).getNumber();
    Specification<Employee> fetchWithCondition =
        fetch(phones, LEFT, (root, query, builder) -> root.get(number).in(phone));
    List<Employee> results = employeeRepository.findAll(fetchWithCondition);
    assertThat(results, hasSize(1));
    Employee employee = results.get(0);
    assertThat(employee.getFirstName(), equalTo(name));
    assertThat(employee.getPhones(), hasSize(1));
    assertThat(employee.getPhones(), hasItem(hasProperty(number, equalTo(phone))));
    assertThat(employee.getPhones(), not(hasItem(hasProperty(number, equalTo(notContainPhone)))));

    FbSpecBuilder<Employee> conds = builder();
    conds.spec(join(phones, eq(number, phone)));
    conds.graph(helper.createGraph(Employee.class, phones));
    results = employeeRepository.findAll(conds.build());
    assertThat(results.get(0).getPhones(), hasSize(1));
  }

  @Test
  public void
      whenGettingListOfEmployeesWithFetchAndSeparatedCondition_thenCorrectWithAllRowsFetched() {
    String name = employeeHangNguyen.getFirstName();
    String phone = employeeHangNguyen.getPhones().get(0).getNumber();
    Specification<Employee> fetch = fetch(phones);
    Specification<Employee> condition =
        join(phones, (root, query, builder) -> root.get(number).in(phone));
    List<Employee> results = employeeRepository.findAll(fetch.and(condition));
    assertThat(results, hasSize(1));
    Employee employee = results.get(0);
    assertThat(employee.getFirstName(), equalTo(name));
    assertThat(employee.getPhones(), hasSize(2));
    assertThat(employee.getPhones(), hasItem(hasProperty(number, equalTo(phone))));

    FbSpecBuilder<Employee> conds = builder();
    conds.spec(
        subquery(
            Phone.class,
            (root, query, subroot, subquery, builder) -> {
              subquery.select(subroot.get(owner).get(Fields.id));
              subquery.where(subroot.get(number).in(phone));
              return root.get(id).in(subquery);
            }));
    conds.spec(fetch(phones));
    results = employeeRepository.findAll(conds.build());
    assertThat(results.get(0).getPhones(), hasSize(2));

    conds = builder();
    conds.spec(
        subquery(
            Phone.class,
            (root, query, subroot, subquery, builder) -> {
              subquery.select(subroot.get(owner).get(Fields.id));
              subquery.where(subroot.get(number).in(phone));
              return root.get(id).in(subquery);
            }));
    conds.graph(helper.createGraph(Employee.class, phones));
    results = employeeRepository.findAll(conds.build());
    assertThat(results.get(0).getPhones(), hasSize(2));
  }

  @Test
  public void givenPhone_whenGettingListOfEmployees_thenFailByLazyLoadOutOfTransaction() {
    String phone = "4";
    Specification<Employee> specification =
        (root, query, builder) -> builder.equal(root.join(phones).get(number), phone);
    List<Employee> results = employeeRepository.findAll(specification);
    assertThat(results, hasSize(1));
    List<Phone> employeePhones = results.get(0).getPhones();
    assertThat(catchThrowable(employeePhones::size), instanceOf(LazyInitializationException.class));
  }

  @Test
  @Transactional
  public void givenPhone_whenGettingListOfEmployeesByJoin_thenLazyLoadCorrectInTransaction() {
    String phone1 = "4";
    String phone2 = "5";
    Specification<Employee> specification =
        (root, query, builder) -> {
          query.distinct(true);
          var number = root.join(phones).get(Fields.number);
          return builder.or(builder.equal(number, phone1), builder.equal(number, phone2));
        };
    List<Employee> results = employeeRepository.findAll(specification);
    assertThat(results, hasSize(1));
    List<Phone> employeePhones = results.get(0).getPhones();
    assertThat(
        employeePhones,
        containsInAnyOrder(
            hasProperty(number, equalTo(phone1)), hasProperty(number, equalTo(phone2))));
  }

  @Test
  public void givenComplexPhone_whenGettingListOfEmployeesByJHQLJointFetch_thenCorrect() {
    String phone1 = "4";
    String phone2 = "5";

    List<Employee> results =
        helper
            .getEm()
            .createQuery(
                "select distinct e from Employee e left join fetch e.phones p where p.number in :numbers",
                Employee.class)
            .setParameter("numbers", List.of(phone1, phone2))
            .getResultList();

    assertThat(results, hasSize(1));
    assertThat(
        results.get(0).getPhones(),
        containsInAnyOrder(
            hasProperty(number, equalTo(phone1)), hasProperty(number, equalTo(phone2))));
  }

  @Test
  public void
      givenComplexPhone_whenGettingListOfEmployeesByJHQLJointFetchThroughRepo_thenCorrect() {
    String phone1 = "4";
    String phone2 = "5";
    List<Employee> results = employeeRepository.getEmployeesWithPhone(List.of(phone1, phone2));

    assertThat(results, hasSize(1));
    assertThat(
        results.get(0).getPhones(),
        containsInAnyOrder(
            hasProperty(number, equalTo(phone1)), hasProperty(number, equalTo(phone2))));
  }

  @Test
  public void givenComplexPhone_whenGettingListOfEmployeesByJHQLGraphThroughRepo_thenCorrect() {
    String phone1 = "4";
    String phone2 = "5";
    List<Employee> results = employeeRepository.getEmployeesWithPhoneGraph(List.of(phone1, phone2));

    assertThat(results, hasSize(1));
    assertThat(
        results.get(0).getPhones(),
        containsInAnyOrder(
            hasProperty(number, equalTo(phone1)), hasProperty(number, equalTo(phone2))));
  }

  @Test
  public void whenGettingListOfEmployeeseWithIdOnly_thenCorrect() {
    CriteriaBuilder cb = helper.getEm().getCriteriaBuilder();
    CriteriaQuery<EmployeeWithIdOnly> query = cb.createQuery(EmployeeWithIdOnly.class);
    Root<Employee> root = query.from(Employee.class);

    String name = employeeHangNguyen.getFirstName();
    Predicate p1 = cb.equal(root.get(firstName), name);
    CriteriaQuery<EmployeeWithIdOnly> cq = query.multiselect(root.get(id)).where(p1);
    List<EmployeeWithIdOnly> results = helper.getEm().createQuery(cq).getResultList();

    assertThat(results, hasSize(1));
    assertThat(results.get(0).getId(), equalTo(employeeHangNguyen.getId()));
  }

  @Test
  public void givenComplexPhone_whenGettingListOfEmployeeIdsByNativeQuery_thenCorrect() {
    String phone1 = "4";
    String phone2 = "5";

    QueryDto<UUID> dto =
        QueryDto.<UUID>builder()
            .query(findEmployeesIdByPhones)
            .resultClass(UUID.class)
            .config(query -> query.setParameter("numbers", List.of(phone1, phone2)))
            .config(query -> query.addScalar("id", PostgresUUIDType.INSTANCE))
            .transformer(PassThroughResultTransformer.INSTANCE)
            .build();
    List<UUID> results = helper.nativeQuery(dto);

    assertThat(results, hasSize(1));
    assertThat(results.get(0), equalTo(employeeHangNguyen.getId()));
  }

  @Test
  @Transactional
  public void
      givenComplexPhone_whenGettingListOfEmployeesByNativeQueryWithLazyLoading_thenCorrect() {
    String phone1 = "4";
    String phone2 = "5";

    List<Employee> results =
        helper.nativeQuery(
            findEmployeesByPhones,
            Employee.class,
            query -> query.setParameter("numbers", List.of(phone1, phone2)));

    assertThat(results, hasSize(1));
    assertThat(
        results.get(0).getPhones(),
        containsInAnyOrder(
            hasProperty(number, equalTo(phone1)), hasProperty(number, equalTo(phone2))));
  }

  @Test
  public void
      givenComplexPhone_whenGettingListOfEmployeesByNativeQueryWithEagerLoading_thenCorrect() {
    String phone1 = "4";
    String phone2 = "5";

    QueryDto<Employee> dto =
        QueryDto.<Employee>builder()
            .query(findEmployeesByPhones)
            .resultClass(Employee.class)
            .config(query -> query.setParameter("numbers", List.of(phone1, phone2)))
            .eagerLoad(e -> initialize(e.getPhones()))
            .eagerLoad(e -> initialize(e.getEmployees()))
            .build();
    List<Employee> results = helper.nativeQuery(dto);

    assertThat(results, hasSize(1));
    assertThat(
        results.get(0).getPhones(),
        containsInAnyOrder(
            hasProperty(number, equalTo(phone1)), hasProperty(number, equalTo(phone2))));
  }

  @Test
  public void whenFindBirthdayBetween_thenReturnCorrectNumberOfEmployees() {
    Specification<Employee> birthdaySpec =
        between(
            birthday,
            ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
            ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
    Set<String> names = Set.of("Nam", "Minh");

    List<Employee> results = employeeRepository.findAll(where(birthdaySpec));

    assertEquals(2, results.size());
    assertTrue(names.contains(results.get(0).getFirstName()));
    assertTrue(names.contains(results.get(1).getFirstName()));
  }

  @Test
  public void givenMinBirthdayNull_whenFindWithinDateRange_thenReturnOneEmployee() {
    Specification<Employee> birthdaySpec =
        between(birthday, null, ZonedDateTime.of(1990, 7, 15, 0, 0, 0, 0, ZoneId.systemDefault()));

    List<Employee> results = employeeRepository.findAll(where(birthdaySpec));

    assertEquals(1, results.size());
    assertEquals("Nam", results.get(0).getFirstName());
  }
}
