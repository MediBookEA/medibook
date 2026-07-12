INSERT INTO doctor (name, specialty) VALUES ('Dr. Sarah Smith', 'Cardiology');
INSERT INTO doctor (name, specialty) VALUES ('Dr. James Johnson', 'Dermatology');
INSERT INTO doctor (name, specialty) VALUES ('Dr. Emily Williams', 'General Practice');

-- Dr. Smith: Mon–Fri 09:00–17:00
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'MONDAY',    '09:00:00', '17:00:00' FROM doctor WHERE name = 'Dr. Sarah Smith';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'TUESDAY',   '09:00:00', '17:00:00' FROM doctor WHERE name = 'Dr. Sarah Smith';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'WEDNESDAY', '09:00:00', '17:00:00' FROM doctor WHERE name = 'Dr. Sarah Smith';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'THURSDAY',  '09:00:00', '17:00:00' FROM doctor WHERE name = 'Dr. Sarah Smith';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'FRIDAY',    '09:00:00', '17:00:00' FROM doctor WHERE name = 'Dr. Sarah Smith';

-- Dr. Johnson: Mon/Wed/Fri 10:00–18:00
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'MONDAY',    '10:00:00', '18:00:00' FROM doctor WHERE name = 'Dr. James Johnson';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'WEDNESDAY', '10:00:00', '18:00:00' FROM doctor WHERE name = 'Dr. James Johnson';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'FRIDAY',    '10:00:00', '18:00:00' FROM doctor WHERE name = 'Dr. James Johnson';

-- Dr. Williams: Tue/Thu 08:00–16:00
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'TUESDAY',  '08:00:00', '16:00:00' FROM doctor WHERE name = 'Dr. Emily Williams';
INSERT INTO working_hours (doctor_id, day_of_week, start_time, end_time) SELECT id, 'THURSDAY', '08:00:00', '16:00:00' FROM doctor WHERE name = 'Dr. Emily Williams';
