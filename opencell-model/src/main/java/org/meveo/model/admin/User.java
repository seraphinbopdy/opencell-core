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
package org.meveo.model.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.meveo.model.AuditableCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.IReferenceEntity;
import org.meveo.model.ISearchable;
import org.meveo.model.ObservableEntity;
import org.meveo.model.ReferenceIdentifierCode;
import org.meveo.model.ReferenceIdentifierDescription;
import org.meveo.model.security.Role;
import org.meveo.model.shared.NameInfo;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;

/**
 * Application user
 * 
 * @author Abdellatif BARI
 * @lastModifiedVersion 7.0
 */
@Entity
@ObservableEntity
@Cacheable
@CustomFieldEntity(cftCodePrefix = "User")
@ExportIdentifier({ "userName" })
@ReferenceIdentifierCode("userName")
@ReferenceIdentifierDescription("email")
@Table(name = "adm_user")
@GenericGenerator(name = "ID_GENERATOR", type = org.hibernate.id.enhanced.SequenceStyleGenerator.class, parameters = { @Parameter(name = "sequence_name", value = "adm_user_seq"),
        @Parameter(name = "increment_size", value = "1") })
@NamedQueries({ @NamedQuery(name = "User.getByUsername", query = "SELECT u FROM User u WHERE lower(u.userName)=:username", hints = { @QueryHint(name = "org.hibernate.cacheable", value = "TRUE") }), })
public class User extends AuditableCFEntity implements IReferenceEntity, ISearchable {

    private static final long serialVersionUID = 5664880105478197047L;

    /**
     * User name
     */
    @Embedded
    private NameInfo name;

    /**
     * Login name
     */
    @Column(name = "username", length = 50, unique = true)
    @Size(max = 50)
    private String userName;

    /**
     * Email
     */
    @Column(name = "email", length = 100)
    @Size(max = 100)
    private String email;

    /**
     * Password
     */
    @Transient
    private String password;

    /**
     * Roles held by the user
     */
    @Transient
    private Set<String> roles = new HashSet<String>();

    /**
     * Roles held by the user
     */
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "adm_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> userRoles = new HashSet<>();

    /**
     * User group
     */
    @Transient
    private String userLevel;

    /**
     * Code
     */
    @Transient
    private String code;

    /**
     * Description
     */
    @Transient
    private String description;

    public User() {
    }

    /**
     * Constructor
     * 
     * @param username Username
     * @param firstName First name
     * @param lastName last name
     * @param email Email
     * @param groups User groups
     * @param roles Roles
     */
    public User(String username, String firstName, String lastName, String email, List<String> groups, List<String> roles) {
        this.userName = username;
        this.name = new NameInfo(null, firstName, lastName);
        this.email = email;
        if (groups != null && !groups.isEmpty()) {
            userLevel = groups.get(0);
        }
        if (roles != null) {
            this.roles = new HashSet<String>(roles);
        }
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> val) {
        this.roles = val;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public NameInfo getName() {
        return name;
    }

    public void setName(NameInfo name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return 961 + (("User" + (userName == null ? "" : userName)).hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof User)) {
            return false;
        }

        User other = (User) obj;

        if (getId() != null && other.getId() != null && getId().equals(other.getId())) {
            return true;
        }

        if (userName == null) {
            if (other.getUserName() != null) {
                return false;
            }
        } else if (!userName.equals(other.getUserName())) {
            return false;
        }
        return true;
    }

    public String toString() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNameOrUsername() {
        if (name != null && name.getFullName().length() > 0) {
            return name.getFullName();
        }

        return userName;
    }

    @Override
    public String getReferenceCode() {
        return getUserName();
    }

    @Override
    public void setReferenceCode(Object value) {
        setUserName(value.toString());
    }

    @Override
    public String getReferenceDescription() {
        return getUserName();
    }

    @Override
    public String getCode() {
        return getUserName();
    }

    @Override
    public void setCode(String code) {

    }

    @Override
    public String getDescription() {
        return "User " + getCode();
    }

    @Override
    public void setDescription(String description) {

    }

    public Set<Role> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<Role> userRoles) {
        this.userRoles = userRoles;
    }

}