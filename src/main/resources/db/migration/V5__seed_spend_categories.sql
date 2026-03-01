-- INFLOW
INSERT INTO spend_categories (name, type, description) VALUES
('Salary',          'INFLOW',  'Regular employment income'),
('Freelance',       'INFLOW',  'Freelance or contract income'),
('Investment',      'INFLOW',  'Returns from investments'),
('Gift',            'INFLOW',  'Monetary gifts received'),
('Other Income',    'INFLOW',  'Any other inflow not covered above');

-- EXPENSE
INSERT INTO spend_categories (name, type, description) VALUES
('Dining',          'EXPENSE', 'Restaurants, cafes and takeaway'),
('Groceries',       'EXPENSE', 'Supermarket and food shopping'),
('Transport',       'EXPENSE', 'Fuel, public transport, ride sharing'),
('Utilities',       'EXPENSE', 'Electricity, water, internet, phone'),
('Entertainment',   'EXPENSE', 'Streaming, events, hobbies'),
('Health',          'EXPENSE', 'Medical, pharmacy, gym'),
('Education',       'EXPENSE', 'Courses, books, subscriptions'),
('Shopping',        'EXPENSE', 'Clothing, electronics, general retail'),
('Housing',         'EXPENSE', 'Rent, mortgage, maintenance'),
('Other',           'EXPENSE', 'Any other expense not covered above');

