/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core.facade;

import static io.lumeer.test.util.LumeerAssertions.assertPermissions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.model.MorphiaOrganization;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermission;
import io.lumeer.storage.mongodb.model.embedded.MorphiaPermissions;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ProjectFacadeIT extends IntegrationTestBase {

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationDao organizationDao;

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String STRANGER_USER = "stranger@nowhere.com";
   private static final String GROUP = "testGroup";

   private static final String CODE1 = "TPROJ1";
   private static final String CODE2 = "TPROJ2";
   private static final String CODE3 = "TPROJ3";

   private static final String NAME = "Testing project";
   private static final String COLOR = "#ff0000";
   private static final String ICON = "fa-search";

   private static final Permission USER_PERMISSION;
   private static final Permission USER_READONLY_PERMISSION;
   private static final Permission GROUP_PERMISSION;
   private static final Permission STRANGER_PERMISSION;

   private static final String ORGANIZATION_CODE = "TORG";

   static {
      USER_PERMISSION = new SimplePermission(USER, Project.ROLES);
      USER_READONLY_PERMISSION = new SimplePermission(USER_PERMISSION.getName(), Collections.singleton(Role.READ));
      STRANGER_PERMISSION = new SimplePermission(STRANGER_USER, Collections.singleton(Role.MANAGE));
      GROUP_PERMISSION = new SimplePermission(GROUP, Collections.singleton(Role.READ));
   }

   private Project createProject(String code) {
      Project project = new JsonProject(code, NAME, ICON, COLOR, null, null);
      project.getPermissions().updateUserPermissions(USER_PERMISSION);
      project.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return projectDao.createProject(project);
   }

   private Project createProjectWithReadOnlyPermissions(final String code) {
      Project project = new JsonProject(code, NAME, ICON, COLOR, null, null);
      project.getPermissions().updateUserPermissions(USER_READONLY_PERMISSION, STRANGER_PERMISSION);
      project.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return projectDao.createProject(project);
   }

   private Project createProjectWithStrangerPermissions(final String code) {
      Project project = new JsonProject(code, NAME, ICON, COLOR, null, null);
      project.getPermissions().updateUserPermissions(
            USER_PERMISSION,
            new SimplePermission(STRANGER_USER, Collections.singleton(Role.MANAGE)));
      project.getPermissions().updateGroupPermissions(GROUP_PERMISSION);
      return projectDao.createProject(project);
   }

   @Before
   public void configureProject() {
      MorphiaOrganization organization = new MorphiaOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new MorphiaPermissions());
      organization.getPermissions().updateUserPermissions(new MorphiaPermission(USER, Role.toStringRoles(new HashSet<>(Arrays.asList(Role.WRITE, Role.READ, Role.MANAGE)))));
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      userDao.createUser(user);

      workspaceKeeper.setOrganization(ORGANIZATION_CODE);
   }

   @Test
   public void testGetProjects() {
      createProject(CODE1);
      createProject(CODE2);

      assertThat(projectFacade.getProjects())
            .extracting(Resource::getCode).containsOnly(CODE1, CODE2);
   }

   @Test
   public void testGetProjectByCode() {
      createProject(CODE1);

      Project storedProject = projectFacade.getProject(CODE1);
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();

      assertPermissions(storedProject.getPermissions().getUserPermissions(), USER_PERMISSION);
   }

   @Test
   public void testDeleteProject() {
      createProject(CODE1);

      projectFacade.deleteProject(CODE1);

      assertThatThrownBy(() -> projectDao.getProjectByCode(CODE1))
            .isInstanceOf(ResourceNotFoundException.class);
   }

   @Test
   public void testCreateProject() {
      Project project = new JsonProject(CODE1, NAME, ICON, COLOR, null, null);

      Project returnedProject = projectFacade.createProject(project);
      assertThat(returnedProject).isNotNull();
      assertThat(returnedProject.getId()).isNotNull();

      Project storedProject = projectDao.getProjectByCode(CODE1);
      assertThat(storedProject).isNotNull();

      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(storedProject.getCode()).isEqualTo(CODE1);
      assertions.assertThat(storedProject.getName()).isEqualTo(NAME);
      assertions.assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertions.assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertions.assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
      assertions.assertThat(storedProject.getPermissions().getGroupPermissions()).isEmpty();
      assertions.assertAll();
   }

   @Test
   public void testUpdateProject() {
      String id = createProject(CODE1).getId();

      Project updatedProject = new JsonProject(CODE2, NAME, ICON, COLOR, null, null);
      updatedProject.getPermissions().removeUserPermission(USER);

      projectFacade.updateProject(CODE1, updatedProject);

      Project storedProject = projectDao.getProjectByCode(CODE2);
      assertThat(storedProject).isNotNull();
      assertThat(storedProject.getId()).isEqualTo(id);
      assertThat(storedProject.getName()).isEqualTo(NAME);
      assertThat(storedProject.getIcon()).isEqualTo(ICON);
      assertThat(storedProject.getColor()).isEqualTo(COLOR);
      assertThat(storedProject.getPermissions().getUserPermissions()).containsOnly(USER_PERMISSION);
   }

   @Test
   public void testGetProjectPermissions() {
      createProject(CODE1);
      createProjectWithReadOnlyPermissions(CODE2);
      createProjectWithStrangerPermissions(CODE3);

      Permissions permissions = projectFacade.getProjectPermissions(CODE1);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);

      permissions = projectFacade.getProjectPermissions(CODE2);
      assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_READONLY_PERMISSION);

      permissions = projectFacade.getProjectPermissions(CODE3);
      assertThat(permissions).isNotNull();
      assertThat(permissions.getUserPermissions()).hasSize(2).contains(USER_PERMISSION, STRANGER_PERMISSION);
   }

   @Test
   public void testUpdateUserPermissions() {
      createProject(CODE1);

      SimplePermission userPermission = new SimplePermission(USER, new HashSet<>(Arrays.asList(Role.MANAGE, Role.READ)));
      projectFacade.updateUserPermissions(CODE1, userPermission);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), userPermission);
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testRemoveUserPermission() {
      createProject(CODE1);

      projectFacade.removeUserPermission(CODE1, USER);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      Assertions.assertThat(permissions.getUserPermissions()).isEmpty();
      assertPermissions(permissions.getGroupPermissions(), GROUP_PERMISSION);
   }

   @Test
   public void testUpdateGroupPermissions() {
      createProject(CODE1);

      SimplePermission groupPermission = new SimplePermission(GROUP, new HashSet<>(Arrays.asList(Role.SHARE, Role.READ)));
      projectFacade.updateGroupPermissions(CODE1, groupPermission);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      assertPermissions(permissions.getGroupPermissions(), groupPermission);
   }

   @Test
   public void testRemoveGroupPermission() {
      createProject(CODE1);

      projectFacade.removeGroupPermission(CODE1, GROUP);

      Permissions permissions = projectDao.getProjectByCode(CODE1).getPermissions();
      Assertions.assertThat(permissions).isNotNull();
      assertPermissions(permissions.getUserPermissions(), USER_PERMISSION);
      Assertions.assertThat(permissions.getGroupPermissions()).isEmpty();
   }
}
