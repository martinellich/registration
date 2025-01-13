package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.SecurityUser;
import ch.martinelli.oss.registration.db.tables.records.SecurityUserRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// @formatter:off
@Repository
public class UserRepository extends JooqDAO<SecurityUser, SecurityUserRecord, Long> {

    public UserRepository(DSLContext dslContext) {
        super(dslContext, SecurityUser.SECURITY_USER);
    }

    public Optional<SecurityUserRecord> findByEmail(String username) {
        return dslContext
                .selectFrom(SecurityUser.SECURITY_USER)
                .where(SecurityUser.SECURITY_USER.EMAIL.eq(username))
                .fetchOptional();
    }

}
