package cn.authing.permission;

import cn.authing.core.Client;
import cn.authing.core.Role;
import cn.authing.core.UserInfo;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {

    public static boolean hasRole(UserInfo userInfo, String role) {
        String[] roleCodes = role.split(" ");

        Client client = new Client();
        List<Role> userRoles = client.getRoles(userInfo);
        if (userRoles == null || userRoles.size() == 0) {
            return false;
        }

        boolean hasRole = false;
        outer:
        for (String code : roleCodes) {
            for (Role r : userRoles) {
                if (code.trim().equals(r.getCode())) {
                    hasRole = true;
                    break outer;
                }
            }
        }

        return hasRole;
    }

    public static boolean canAccessResource(UserInfo userInfo, String value) {
        Client client = new Client();
        List<Resource> resources = client.listResources(userInfo);
        if (resources == null || resources.size() == 0) {
            return false;
        }

        String res;
        String action = "*";
        String[] split = value.split(":");
        res = split[0];
        if (split.length > 1) {
            action = split[1];
        }

        List<Resource> resourceFound = findResource(resources, res);
        for (Resource r : resourceFound) {
            for (String a : r.getActions()) {
                String atomicAction = getAction(a);
                if (StringUtils.hasLength(atomicAction)) {
                    if (atomicAction.equals("*") || atomicAction.equals(action)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static List<Resource> findResource(List<Resource> resources, String res) {
        List<Resource> ret = new ArrayList<>();
        for (Resource r : resources) {
            String code = r.getCode();
            if (!StringUtils.hasLength(code)) {
                continue;
            }
            String[] split = code.split(":");
            if (split.length == 0) {
                continue;
            }
            if (split[0].equals(res)) {
                ret.add(r);
            }
        }
        return ret;
    }

    public static String getAction(String raw) {
        if ("*".equals(raw)) {
            return raw;
        }

        String[] split = raw.split(":");
        if (split.length > 1) {
            return split[1];
        }
        return null;
    }
}
