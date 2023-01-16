with ct1 as (select *
             from tb_events
             where config_id = 2
               and type = 'POWER'
             order by created),
     ct2 as (select *
             from tb_events
             where config_id = 2
               and type = 'POWER'
             order by created
             offset 1),
     t1 AS
             (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT 1)) AS tempId, * FROM ct1),
     t2 AS
             (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT 1)) AS tempId, * FROM ct2),
     ids as
         (select t1.created                                     as c1,
                 t1.id                                          as id1,
                 t2.created                                     as c2,
                 t2.id                                          as id2,
                 ((DATE_PART('day', t2.created - t1.created) * 24 + DATE_PART('hour', t2.created - t1.created)) * 60 +
                  DATE_PART('minute', t2.created - t1.created)) as diff
          from t1
                   left join t2 on t2.tempId = t1.tempId
          where ((DATE_PART('day', t2.created - t1.created) * 24 + DATE_PART('hour', t2.created - t1.created)) * 60 +
                 DATE_PART('minute', t2.created - t1.created)) < 5)

select id1, c1
from ids
union
select id2, c2
from ids
order by c1
