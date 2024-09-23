/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */
package org.meveo.service.admin.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ElementNotFoundException;
import org.meveo.admin.exception.InvalidParameterException;
import org.meveo.admin.exception.UsernameAlreadyExistsException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.security.client.KeycloakAdminClientService;
import org.meveo.service.base.PersistenceService;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

/**
 * User service implementation.
 */
@Stateless
@DeclareRoles({ "userManagement", "userSelfManagement", "apiUserManagement", "apiUserSelfManagement" })
public class UserService extends PersistenceService<User> {

    @Inject
    KeycloakAdminClientService keycloakAdminClientService;

    @Inject
    protected ParamBeanFactory paramBeanFactory;

    /**
     * Identifies the source of user and role information
     * 
     * @author Andrius Karpavicius
     */
    public enum UserManagementMasterEnum {
        /**
         * The primary source of users and role information is Opencell. Users and roles in Keycloak are limited to authentication purpose and user and role information in Keycloak should come from external systems e.g.
         * LDAP <br/>
         * Users are created/updated to KC, but for the consultation, we only retrieve the users that are in Opencell database.<br/>
         * For the roles, we create them only on Opencell side as they are not authorization roles.
         */
        OC(true, true, true, false, false, false),

        /**
         * The primary source of users and roles is Keycloak.<br/>
         * Opencell stores only custom field and secured entity information, records in adm_user nor adm_role table are not required.<br/>
         * When Users and roles are managed from Opencell API/GUI side, and user and role information <b>IS send</b> to Keycloak
         */
        KC(false, false, false, true, true, true),

        /**
         * The <b>ONLY</b> source of users and roles is Keycloak. User and role information in Keycloak should come from external systems e.g. LDAP<br/>
         * Opencell stores only custom field and secured entity information, records in adm_user nor adm_role table are not required.<br/>
         * When Users and roles are managed from Opencell API/GUI side, and user and role information is <b>NOT send</b> to Keycloak
         */
        KC_USER_READ_ONLY(false, false, false, false, true, false);

        /**
         * Shall user information be duplicated in Opencell, and preferred over user information in Keycloak for consultation purpose.
         */
        private boolean isOcUserDuplicate;

        /**
         * Shall roles held by the user be duplicated in Opencell and "joined" with roles held by the user in Keycloak.
         */
        private boolean isOcUserRolesDuplicate;

        /**
         * Shall Opencell configure additional roles that are not required by Keycloak for authorization. E.g. business type roles used in local scripts, reports, etc..
         */
        private boolean isOcRoleDuplicate;

        /**
         * Is user information in Keycloak is writable from Opencell User api. If false, user in Keycloak should be created by some other means - created directly in Keycloak or federated from LDAP, etc.
         */
        private boolean isKcUserWrite;

        /**
         * Are roles held by the user in Keycloak are assignable from Opencell User api. If false, roles to the user in Keycloak should be assigned by some other means - assigned directly in Keycloak or federated from
         * LDAP via groups, etc.
         */
        private boolean isKcUserRolesWrite;

        /**
         * Is role information in Keycloak is writable from Opencell Role api. If false, roles in Keycloak should be configured directly in Keycloak application.
         */
        private boolean isKcRoleWrite;

        /**
         * Constructor
         * 
         * @param isOcUserDuplicate Shall user information be duplicated in Opencell, and preferred over user information in Keycloak for consultation purpose.
         * @param isOcUserRolesDuplicate Shall roles held by the user be duplicated in Opencell and "joined" with roles held by the user in Keycloak.
         * @param isOcRolesDuplicate Shall Opencell configure additional roles that are not required by Keycloak for authorization. E.g. business type roles used in local scripts, reports, etc..
         * @param isKcUserWrite Is user information in Keycloak is writable from Opencell User api. If false, user in Keycloak should be created by some other means - created directly in Keycloak or federated from LDAP,
         *        etc.
         * @param isKcUserRolesWrite Are roles held by the user in Keycloak are assignable from Opencell User api. If false, roles to the user in Keycloak should be assigned by some other means - assigned directly in
         *        Keycloak or federated from LDAP via groups, etc.
         * @param isKcRolesWrite Is role information in Keycloak is writable from Opencell Role api. If false, roles in Keycloak should be configured directly in Keycloak application.
         */
        private UserManagementMasterEnum(boolean isOcUserDuplicate, boolean isOcUserRolesDuplicate, boolean isOcRolesDuplicate, boolean isKcUserWrite, boolean isKcUserRolesWrite, boolean isKcRolesWrite) {
            this.isOcUserDuplicate = isOcUserDuplicate;
            this.isOcUserRolesDuplicate = isOcUserRolesDuplicate;
            this.isOcRoleDuplicate = isOcRolesDuplicate;
            this.isKcUserWrite = isKcUserWrite;
            this.isKcUserRolesWrite = isKcUserRolesWrite;
            this.isKcRoleWrite = isKcRolesWrite;
    }

        /**
         * @return Shall user information be duplicated in Opencell, and preferred over user information in Keycloak for consultation purpose.
         */
        public boolean isOcUserDuplicate() {
            return isOcUserDuplicate;
        }

        /**
         * @return Shall roles held by the user be duplicated in Opencell and "joined" with roles held by the user in Keycloak.
         */
        public boolean isOcUserRolesDuplicate() {
            return isOcUserRolesDuplicate;
        }

        /**
         * @return Shall Opencell configure additional roles that are not required by Keycloak for authorization. E.g. business type roles used in local scripts, reports, etc..
         */
        public boolean isOcRoleDuplicate() {
            return isOcRoleDuplicate;
        }

        /**
         * @return Is user information in Keycloak is writable from Opencell User api. If false, user in Keycloak should be created by some other means - created directly in Keycloak or federated from LDAP, etc.
         */
        public boolean isKcUserWrite() {
            return isKcUserWrite;
        }

        /**
         * @return Are roles held by the user in Keycloak are assignable from Opencell User api. If false, roles to the user in Keycloak should be assigned by some other means - assigned directly in Keycloak or federated
         *         from LDAP via groups, etc.
         */
        public boolean isKcUserRolesWrite() {
            return isKcUserRolesWrite;
        }

        /**
         * @return Is role information in Keycloak is writable from Opencell Role api. If false, roles in Keycloak should be configured directly in Keycloak application.
         */
        public boolean isKcRoleWrite() {
            return isKcRoleWrite;
        }

    }

    @Override
    @RolesAllowed({ "userManagement", "userSelfManagement", "apiUserManagement", "apiUserSelfManagement" })
    public void create(User user) throws UsernameAlreadyExistsException, InvalidParameterException {

        user.setUserName(user.getUserName().toUpperCase());

        // Create user in in Keycloak if applicable
        // If returned userId is null that means that user was not created in KC because a current user management source is configured to not create users in Keycloak
        String userId = keycloakAdminClientService.createUser(user.getUserName(), user.getName().getFirstName(), user.getName().getLastName(), user.getEmail(), user.getPassword(), user.getUserLevel(),
            user.getRolesByClient(), null, user.getAttributes());

        User userInDb = getUserFromDatabase(user.getUserName());
        
        // Create user in Opencell DB if applicable
        if (userId == null && userInDb != null && getUserManagementMaster().isOcUserDuplicate()) {
        		throw new EntityAlreadyExistsException(User.class, user.getUserName(), "username");

        } else if (userInDb == null) {
        	super.create(user);
        }
    }

    @Override
    @RolesAllowed({ "userManagement", "userSelfManagement", "apiUserManagement", "apiUserSelfManagement" })
    public User update(User user) throws ElementNotFoundException, InvalidParameterException {
        return update(user, true);
        }

    /**
     * Update user in Opencell DB and Keycloak
     * 
     * @param user User to update
     * @param isReplaceRoles Is this a final list of roles held by user (True). False - roles will be appended to the currently assigned roles.
     * @return User updated
     * @throws ElementNotFoundException
     * @throws InvalidParameterException
     */
    @RolesAllowed({ "userManagement", "userSelfManagement", "apiUserManagement", "apiUserSelfManagement" })
    public User update(User user, boolean isReplaceRoles) throws ElementNotFoundException, InvalidParameterException {

        user.setUserName(user.getUserName().toUpperCase());
		String firstName = user.getName() != null ? user.getName().getFirstName() : null;
		String lastName = user.getName() != null ? user.getName().getLastName() : null;

        // Update information in Keycloak
        keycloakAdminClientService.updateUser(user.getUserName(), firstName, lastName, user.getEmail(), user.getPassword(), user.getUserLevel(), user.getRolesByClient(), isReplaceRoles, user.getAttributes());

        // And in Opencell DB
		 if(user.getId() != null) {
            return super.update(user);
        } else {
            return user;
        }
    }

    @Override
    @RolesAllowed({ "userManagement", "apiUserManagement" })
    public void remove(User user) throws BusinessException {
        keycloakAdminClientService.deleteUser(user.getUserName());
        super.remove(user);
    }

    /**
     * Lookup a user by a username in Keycloak or Opencell DB
     * 
     * @param username Username to lookup by
     * @param extendedInfo Shall group membership and roles be retrieved
     * @return User found
     */
    public User findByUsername(String username, boolean extendedInfo, boolean extendedClientRoles) {
        User lUser = null;
	    
        // Preference taken to user information from Opencell DB
        if (getUserManagementMaster().isOcUserDuplicate()) {

            lUser = getUserFromDatabase(username);
            if (lUser == null) {
		    lUser = keycloakAdminClientService.findUser(username, extendedInfo, extendedClientRoles);

        } else {
			
                if (extendedInfo) {
				this.fillKeycloakUserInfo(lUser);
			}
                if (extendedClientRoles) {
                    Map<String, List<String>> clientRoles = keycloakAdminClientService.getClientRoles(username);
                    lUser.addClientLevelRoles(clientRoles);
		}
            }

        } else {

            lUser = keycloakAdminClientService.findUser(username, extendedInfo, extendedClientRoles);
        }
        return lUser;
    }

    public User getUserFromDatabase(String pUserName) {
        User lUser = null;
        try {
            lUser = getEntityManager().createNamedQuery("User.getByUsername", User.class).setParameter("username", pUserName.toLowerCase()).getSingleResult();
        } catch (NoResultException ex) {
        }

        return lUser;
    }

    @Override
    public List<User> list(PaginationConfiguration config) {
        List<User> users = new ArrayList<>();

        // Preference is taken to the user information from Opencell DB when OC user duplicate mode is activated
        if (getUserManagementMaster().isOcUserDuplicate()) {
            String firstName = (String) config.getFilters().get("name.firstName");
            String lastName = (String) config.getFilters().get("name.lastName");
            String email = (String) config.getFilters().get("email");

            if(StringUtils.isBlank(firstName)) {
                this.removeFilters(config, "name.firstName");
            }

            if(StringUtils.isBlank(lastName)) {
                this.removeFilters(config, "name.lastName");
            }

            if(StringUtils.isBlank(email)) {
                this.removeFilters(config, "email");
            }

            users = super.list(config);

            // Load attributes and client roles if requested
            for (User user : users) {
                if (config.getFetchFields() != null && config.getFetchFields().contains("attributes")) {
                    user.setAttributes(keycloakAdminClientService.getUserAttributesByUsername(user.getUserName()));
                }
                if (config.getFetchFields() != null && config.getFetchFields().contains("clientRoles")) {
                    user.addClientLevelRoles(keycloakAdminClientService.getClientRoles(user.getUserName()));
                }
            }

        } else {
            //Get user from keycloak
            users = keycloakAdminClientService.listUsers(config);
            this.removeFilters(config, "name.firstName", "name.lastName", "email");

            // Load client roles if requested
            for (User user : users) {
                if (config.getFetchFields() != null && config.getFetchFields().contains("clientRoles")) {
                    user.addClientLevelRoles(keycloakAdminClientService.getClientRoles(user.getUserName()));
                }
            }

            //Construct a list of names
            List<String> usernamesList = users.stream().map(User::getUserName).collect(Collectors.toList());
            config.getFilters().put("inList userName", usernamesList);

            // Get list of users from database and fill all missing fields
            List<User> lDbUsers = super.list(config);
            users.forEach(keycloakUser -> {
                lDbUsers.forEach(dbUser -> {
                    if(keycloakUser.getUserName().equalsIgnoreCase(dbUser.getUserName())) {
                        fillEmptyFields(keycloakUser, dbUser);
                    }
                });
            });
        }

        return users;
    }

    @Override
    public long count(PaginationConfiguration config) {

        // Preference is taken to the user information from Opencell DB when OC user duplicate mode is activated
        if (getUserManagementMaster().isOcUserDuplicate()) {
            String firstName = (String) config.getFilters().get("name.firstName");
            String lastName = (String) config.getFilters().get("name.lastName");
            String email = (String) config.getFilters().get("email");

            if(StringUtils.isBlank(firstName) && StringUtils.isBlank(lastName) && StringUtils.isBlank(email)) {
                this.removeFilters(config, "name.firstName", "name.lastName", "email");
                return super.count(config);
            } else {
                return super.count(config);
            }

        } else {
            List<User> users = keycloakAdminClientService.listUsers(config);
            return users.size();
        }
    }

    /**
     * Check if user belongs to a group or a higher group
     * 
     * @param belongsToUserGroup A group to check
     * @return True if user belongs to a given group of to a parent of the group
     */
    public boolean isUserBelongsGroup(String belongsToUserGroup) {
        // TODO finish checking the hierarchy
        return belongsToUserGroup.equalsIgnoreCase(currentUser.getUserGroup());
    }

    /**
     * Lookup a user by an id
     *
     * @param extendedInfo Shall group membership and roles be retrieved
     * @return User found
     */
    @Override
    public User findById(Long id, boolean extendedInfo) {
        if(id == null) {
            return null;
        }

        User user = findById(id);
        if (user == null) {
             return null;
        }

        if(extendedInfo) {
            this.fillKeycloakUserInfo(user);
        }

        return user;
    }

    /**
     * Lookup a keycloak user info - realm roles, attributes, user level
     *
     * @param user user
     */
    private void fillKeycloakUserInfo(User user) {
            User kcUser = keycloakAdminClientService.findUser(user.getUserName(), true, false);
        if (kcUser != null) {
            user.addRealmLevelRoles(new ArrayList<String>(kcUser.getRoles()));
            user.setUserLevel(kcUser.getUserLevel());
            user.setAttributes(kcUser.getAttributes());
        }
}

    /**
     * Remove filters from config
     *
     * @param pConfig {@link PaginationConfiguration}
     * @param pKeys A list of keys to remove
     */
    private void removeFilters(PaginationConfiguration pConfig, String ... pKeys) {
        for(String key : pKeys) {
            pConfig.getFilters().remove(key);
        }
    }

    /**
     * Fill Empty field to return it
     * 
     * @param pKeycloakUser {@link User} User returned from keycloak
     * @param pDbUser {@link User} User returned from database
     */
    private void fillEmptyFields(User pKeycloakUser, User pDbUser) {
        if(pKeycloakUser.getEmail() == null && pDbUser.getEmail() != null && !pDbUser.getEmail().isBlank()) {
            pKeycloakUser.setEmail(pDbUser.getEmail());
        }

        if(pKeycloakUser.getUuid() == null && pDbUser.getUuid() != null && !pDbUser.getUuid().isEmpty()) {
            pKeycloakUser.setUuid(pDbUser.getUuid());
        }
    }

    /**
     * Get a single attribute value from a user
     * 
     * @param username Username
     * @param attributeName Name of attribute to retrieve
     * @return A value of attribute. In case of multiple attribute values, values are concatenated by a comma.
     */
    public String getUserAttributeValue(String username, String attributeName) {
        return keycloakAdminClientService.getUserAttributeValue(username, attributeName);
                    }
	
    /**
     * Get user/role management division between Opencell and Keycloak applications
     * 
     * @return userManagement.master configuration property value
     */
    public UserManagementMasterEnum getUserManagementMaster() {
        String userManagementSource = ParamBean.getInstance().getProperty("userManagement.master", UserManagementMasterEnum.KC.name());
        UserManagementMasterEnum master = UserManagementMasterEnum.KC;
        try {
            master = UserManagementMasterEnum.valueOf(userManagementSource);
        } catch (Exception e) {
            log.error("Unrecognized 'userManagement.master' property value. A default value of 'KC' will be used.");
	}
        return master;
    }
}