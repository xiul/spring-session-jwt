package com.github.session.jwt;

import com.github.session.jwt.config.SpringSessionConfig;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EnableEncryptableProperties
@EnableRedisHttpSession
@Component
@Slf4j
public class SpringSessionJwt {

    @Autowired
    private SpringSessionConfig config;

    @Autowired
    private HttpSession session;

    public Map<String, String> get() {
        String jwt = (String) session.getAttribute(config.getJwtSessionKey());
        if (StringUtils.isEmpty(jwt)) {
            throw new JwtException("No jwt on session (session-key: " + config.getJwtSessionKey() + ")");
        } else {
            return get(jwt);
        }
    }

    public Map<String, String> get(String jwt) {
        byte[] key = Base64Utils.decodeFromUrlSafeString(config.getJwtSecret());
        Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jwt);
        Claims body = claims.getBody();

        Map<String, String> values = new HashMap<>();
        body.keySet().forEach(claimKey -> {
            Object value = body.get(claimKey);
            values.put(claimKey, value.toString());
        });

        return values;
    }

    public Optional<String> getJwt() {
        Object jwt = session.getAttribute(config.getJwtSessionKey());
        if (jwt != null) {
            try {
                String jwtString = (String) jwt;
                byte[] key = Base64Utils.decodeFromUrlSafeString(config.getJwtSecret());
                Jwts.parser().setSigningKey(key).parse(jwtString);
                return Optional.of((String) jwt);
            } catch (JwtException e) {
                log.warn("JWT is not valid", e);
            }
        }

        return Optional.empty();
    }

    public boolean isValidJwt() {
        Optional<String> jwt = getJwt();
        return jwt.isPresent();
    }
}