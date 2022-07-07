select e.*
from employee e
where exists (select 1 from phone p where p.owner_id = e.id and p.number in (:numbers))