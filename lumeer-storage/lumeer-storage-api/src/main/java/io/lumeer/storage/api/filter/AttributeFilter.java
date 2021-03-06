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
package io.lumeer.storage.api.filter;

import io.lumeer.api.model.ConditionType;

import java.util.Objects;

public class AttributeFilter {

   private final String collectionId;
   private final ConditionType conditionType;
   private final String attributeName;
   private final String value;

   public AttributeFilter(final String collectionId, final ConditionType conditionType, final String attributeName, final String value) {
      this.collectionId = collectionId;
      this.conditionType = conditionType;
      this.attributeName = attributeName;
      this.value = value;
   }

   public String getCollectionId() {
      return collectionId;
   }

   public ConditionType getConditionType() {
      return conditionType;
   }

   public String getValue() {
      return value;
   }

   public String getAttributeName() {
      return attributeName;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeFilter)) {
         return false;
      }
      final AttributeFilter that = (AttributeFilter) o;
      return Objects.equals(getCollectionId(), that.getCollectionId()) &&
            getConditionType() == that.getConditionType() &&
            Objects.equals(getValue(), that.getValue()) &&
            Objects.equals(getAttributeName(), that.getAttributeName());
   }

   @Override
   public int hashCode() {

      return Objects.hash(getCollectionId(), getConditionType(), getValue(), getAttributeName());
   }

   @Override
   public String toString() {
      return "AttributeFilter{" +
            "collectionId='" + collectionId + '\'' +
            ", conditionType=" + conditionType +
            ", value='" + value + '\'' +
            ", attributeName='" + attributeName + '\'' +
            '}';
   }
}
