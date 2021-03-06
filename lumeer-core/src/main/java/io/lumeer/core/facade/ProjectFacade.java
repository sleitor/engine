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

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ProjectFacade extends AbstractFacade {

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   public Project createProject(Project project) {
      checkOrganizationWriteRole();
      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUsername(), Project.ROLES);
      project.getPermissions().updateUserPermissions(defaultUserPermission);

      Project storedProject = projectDao.createProject(project);

      createProjectScopedRepositories(storedProject);

      return storedProject;
   }

   public Project updateProject(final String projectCode, final Project project) {
      Project storedProject = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(storedProject, Role.MANAGE);

      keepStoredPermissions(project, storedProject.getPermissions());
      Project updatedProject = projectDao.updateProject(storedProject.getId(), project);

      return keepOnlyActualUserRoles(updatedProject);
   }

   public void deleteProject(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      deleleProjectScopedRepositories(project);

      projectDao.deleteProject(project.getId());
   }

   public Project getProject(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.READ);

      return keepOnlyActualUserRoles(project);
   }

   public List<Project> getProjects() {
      User user = authenticatedUser.getCurrentUser();
      Set<String> groups = authenticatedUser.getCurrentUserGroups();

      DatabaseQuery query = DatabaseQuery.createBuilder(user.getEmail())
                                         .groups(groups)
                                         .build();

      return projectDao.getProjects(query).stream()
                       .map(this::keepOnlyActualUserRoles)
                       .collect(Collectors.toList());
   }

   public Set<String> getProjectsCodes() {
      return projectDao.getProjectsCodes();
   }

   public Permissions getProjectPermissions(final String projectCode) {
      Project project = projectDao.getProjectByCode(projectCode);

      if (permissionsChecker.hasRole(project, Role.MANAGE)) {
         return project.getPermissions();
      } else if (permissionsChecker.hasRole(project, Role.READ)) {
         return keepOnlyActualUserRoles(project).getPermissions();
      }

      throw new NoPermissionException(project);
   }

   public Set<Permission> updateUserPermissions(final String projectCode, final Permission... userPermissions) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().updateUserPermissions(userPermissions);
      projectDao.updateProject(project.getId(), project);

      return project.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String projectCode, final String user) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().removeUserPermission(user);
      projectDao.updateProject(project.getId(), project);
   }

   public Set<Permission> updateGroupPermissions(final String projectCode, final Permission... groupPermissions) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().updateGroupPermissions(groupPermissions);
      projectDao.updateProject(project.getId(), project);

      return project.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String projectCode, final String group) {
      Project project = projectDao.getProjectByCode(projectCode);
      permissionsChecker.checkRole(project, Role.MANAGE);

      project.getPermissions().removeGroupPermission(group);
      projectDao.updateProject(project.getId(), project);
   }

   private void createProjectScopedRepositories(Project project) {
      collectionDao.createCollectionsRepository(project);
      documentDao.createDocumentsRepository(project);
      viewDao.createViewsRepository(project);
      linkInstanceDao.createLinkInstanceRepository(project);
      linkTypeDao.createLinkTypeRepository(project);
   }

   private void deleleProjectScopedRepositories(Project project) {
      collectionDao.deleteCollectionsRepository(project);
      documentDao.deleteDocumentsRepository(project);
      viewDao.deleteViewsRepository(project);
      linkTypeDao.deleteLinkTypeRepository(project);
      linkInstanceDao.deleteLinkInstanceRepository(project);
   }

   private void checkOrganizationWriteRole() {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      Organization organization = workspaceKeeper.getOrganization().get();
      permissionsChecker.checkRole(organization, Role.WRITE);
   }
}
