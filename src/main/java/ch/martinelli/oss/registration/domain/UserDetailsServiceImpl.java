package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.SecurityUserRecord;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static ch.martinelli.oss.registration.db.Tables.SECURITY_GROUP;
import static ch.martinelli.oss.registration.db.tables.SecurityUser.SECURITY_USER;
import static ch.martinelli.oss.registration.db.tables.UserGroup.USER_GROUP;

@Service
@Primary
public class UserDetailsServiceImpl implements UserDetailsService {

    private final DSLContext dslContext;

    public UserDetailsServiceImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SecurityUserRecord securityUserRecord = dslContext
                .selectFrom(SECURITY_USER)
                .where(SECURITY_USER.EMAIL.eq(username))
                .fetchOne();

        if (securityUserRecord != null) {
            Result<Record1<String>> groups = dslContext
                    .select(USER_GROUP.securityGroup().NAME)
                    .from(USER_GROUP)
                    .where(USER_GROUP.USER_ID.eq(securityUserRecord.getId()))
                    .fetch();

            return new User(securityUserRecord.getEmail(), securityUserRecord.getSecret(),
                groups.stream()
                    .map(group -> new SimpleGrantedAuthority("ROLE_" + group.getValue(SECURITY_GROUP.NAME)))
                    .toList());
        } else {
            throw new UsernameNotFoundException(username);
        }
    }
}
