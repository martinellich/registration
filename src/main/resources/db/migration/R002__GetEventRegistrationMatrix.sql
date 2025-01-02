CREATE OR REPLACE FUNCTION get_event_registration_matrix()
    RETURNS TEXT
    LANGUAGE plpgsql
AS
$$
DECLARE
    dynamic_sql TEXT;
    event_columns TEXT;
BEGIN
    -- Generate the column list for events, now sorted by from_date
    SELECT string_agg(
                   format('BOOL_OR(CASE WHEN e.title = %L THEN er.registered ELSE false END) as %I',
                          title,
                          replace(lower(title), ' ', '_')  -- Convert "Event Name" to "event_name"
                   ),
                   ', '
           )
    INTO event_columns
    FROM (
             SELECT DISTINCT title, from_date
             FROM event
             ORDER BY from_date, title
         ) e;

    -- Build the final query
    dynamic_sql := format('
        SELECT
            p.last_name,
            p.first_name,
            %s
        FROM person p
        LEFT JOIN event_registration er ON er.person_id = p.id
        LEFT JOIN event e ON e.id = er.event_id
        WHERE p.active = true
        GROUP BY p.id, p.last_name, p.first_name
        ORDER BY p.last_name, p.first_name
    ', event_columns);

    RETURN dynamic_sql;
END;
$$;