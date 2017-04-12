/*
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
package com.facebook.presto.sql.planner.optimizations;

import com.facebook.presto.Session;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.sql.planner.PlanNodeIdAllocator;
import com.facebook.presto.sql.planner.Symbol;
import com.facebook.presto.sql.planner.SymbolAllocator;
import com.facebook.presto.sql.planner.plan.AggregationNode;
import com.facebook.presto.sql.planner.plan.PlanNode;
import com.facebook.presto.sql.planner.plan.ProjectNode;
import com.facebook.presto.sql.planner.plan.SemiJoinNode;
import com.facebook.presto.sql.planner.plan.SimplePlanRewriter;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class RemoveDistinctFromSemiJoin
        implements PlanOptimizer
{
    public RemoveDistinctFromSemiJoin()
    {
    }

    @Override
    public PlanNode optimize(PlanNode plan, Session session, Map<Symbol, Type> types, SymbolAllocator symbolAllocator, PlanNodeIdAllocator idAllocator)
    {
        requireNonNull(plan, "plan is null");
        requireNonNull(session, "session is null");
        requireNonNull(types, "types is null");
        requireNonNull(symbolAllocator, "symbolAllocator is null");
        requireNonNull(idAllocator, "idAllocator is null");
        if (session.getSystemProperty("remove_distinct_from_semijoin", Boolean.class)) {
            return SimplePlanRewriter.rewriteWith(new Rewriter(), plan, false);
        }
        return plan;
    }

    private static class Rewriter
            extends SimplePlanRewriter<Boolean>
    {
        public Rewriter()
        {
        }

        @Override
        public PlanNode visitSemiJoin(SemiJoinNode node, RewriteContext<Boolean> context)
        {
            PlanNode sourceRewritten = context.rewrite(node.getSource(), false);
            PlanNode filteringSourceRewritten = context.rewrite(node.getFilteringSource(), true);

            return new SemiJoinNode(
                    node.getId(),
                    sourceRewritten,
                    filteringSourceRewritten,
                    node.getSourceJoinSymbol(),
                    node.getFilteringSourceJoinSymbol(),
                    node.getSemiJoinOutput(),
                    node.getSourceHashSymbol(),
                    node.getFilteringSourceHashSymbol(),
                    node.getDistributionType());
        }
        @Override
        public PlanNode visitAggregation(AggregationNode node, RewriteContext<Boolean> context)
        {
            if (context.get()) {
                if (node.getOutputSymbols().size() == 1
                        && node.getGroupingKeys().size() == 1
                        && node.getOutputSymbols().get(0) == node.getGroupingKeys().get(0)) {
                    return context.rewrite(node.getSource(), false);
                }
            }
            return new AggregationNode(
                    node.getId(),
                    context.rewrite(node.getSource(), false),
                    node.getAssignments(),
                    node.getGroupingSets(),
                    node.getStep(),
                    node.getHashSymbol(),
                    node.getGroupIdSymbol());
        }

        @Override
        public PlanNode visitProject(ProjectNode node, RewriteContext<Boolean> context)

        {
            return context.defaultRewrite(node, context.get());
        }

        @Override
        public PlanNode visitPlan(PlanNode node, RewriteContext<Boolean> context)
        {
            return context.defaultRewrite(node, false);
        }
    }
}
