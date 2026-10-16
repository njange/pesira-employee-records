-- Every value is derived from the row number, so the seed is the same on every run.
-- 11 first names x 10 last names give 110 distinct pairs, which keeps the 60 emails unique.
INSERT INTO employees (first_name, last_name, email, department, job_title, salary, hire_date, status)
SELECT n.first_name,
       n.last_name,
       lower(n.first_name || '.' || n.last_name) || '@example.com',
       (ARRAY['Engineering', 'Finance', 'Human Resources', 'Marketing', 'Sales', 'Operations'])[n.dept],
       (ARRAY['Software Engineer', 'Financial Analyst', 'HR Specialist', 'Marketing Manager', 'Account Executive', 'Operations Analyst'])[n.dept],
       (ARRAY[95000, 72000, 58000, 64000, 66000, 52000])[n.dept] + (i * 1237) % 25000,
       DATE '2015-01-05' + (i * 173) % 3650,
       CASE WHEN i % 15 = 0 THEN 'TERMINATED' WHEN i % 7 = 0 THEN 'ON_LEAVE' ELSE 'ACTIVE' END
FROM generate_series(1, 60) AS i
CROSS JOIN LATERAL (
    SELECT (ARRAY['Amina', 'Brian', 'Wanjiru', 'David', 'Grace', 'Kevin', 'Faith', 'Samuel', 'Mercy', 'Joseph', 'Lucy'])[i % 11 + 1] AS first_name,
           (ARRAY['Otieno', 'Kamau', 'Mwangi', 'Njoroge', 'Achieng', 'Kiprop', 'Wambui', 'Mutua', 'Odhiambo', 'Chebet'])[i % 10 + 1] AS last_name,
           i % 6 + 1 AS dept
) AS n;
