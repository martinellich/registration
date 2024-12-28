package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.SecurityUser;
import ch.martinelli.oss.registration.db.tables.records.SecurityUserRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends JooqDAO<SecurityUser, SecurityUserRecord, Long> {

    public UserRepository(DSLContext dslContext) {
        super(dslContext, SecurityUser.SECURITY_USER);
    }
}
