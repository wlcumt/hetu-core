/*
 * Copyright (C) 2018-2020. Huawei Technologies Co., Ltd. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.sql.tree;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

public class ShowIndex
        extends Statement
{
    private final Optional<QualifiedName> indexName;

    public ShowIndex(NodeLocation location, QualifiedName indexName)
    {
        this(Optional.of(location), Optional.of(indexName));
    }

    public ShowIndex(NodeLocation location)
    {
        this(Optional.of(location), Optional.empty());
    }

    public ShowIndex(Optional<NodeLocation> location, Optional<QualifiedName> indexName)
    {
        super(location);
        this.indexName = indexName;
    }

    public String getIndexName()
    {
        return indexName.isPresent() ? indexName.get().toString() : null;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitShowIndex(this, context);
    }

    @Override
    public List<Node> getChildren()
    {
        return ImmutableList.of();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        return (obj != null) && (getClass() == obj.getClass());
    }

    @Override
    public String toString()
    {
        return toStringHelper(this).toString();
    }
}
