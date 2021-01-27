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
package io.prestosql.operator;

import io.prestosql.spi.Page;
import nova.hetu.omnicache.runtime.OmniRuntime;
import nova.hetu.omnicache.vector.LongVec;
import nova.hetu.omnicache.vector.Vec;
import nova.hetu.omnicache.vector.VecType;

import static com.google.common.base.Preconditions.checkState;

public final class OmniWork<I, O>
        implements Work<O>
{

    OmniRuntime omniRuntime;
    String compileID;
    private boolean finished;
    private O result;
    private final Page page;
    Page tempHashPage;

    public OmniWork(Page page, OmniRuntime omniRuntime, String compileID, Page tempHashPage)
    {
        this.page = page;
        this.omniRuntime = omniRuntime;
        this.compileID = compileID;
        this.tempHashPage=tempHashPage;
    }

    @Override
    public boolean process()
    {
        Vec[] inputData = new Vec[2];
//        inputData[0] = (LongVec) page.getBlock(0).getValuesVec();
//        inputData[1] = (LongVec) page.getBlock(1).getValuesVec();

        for (int i = 0; i < inputData[0].size(); i++) {
            System.out.println("block0 before omni:" + inputData[0].get(i));
            System.out.println("block1 before omni:" + inputData[1].get(i));
        }

        int rowNum = page.getPositionCount();

        VecType[] outTypes = {VecType.LONG, VecType.LONG};
        long start1 = System.currentTimeMillis();

        result = (O) omniRuntime.execute(compileID, inputData, rowNum, outTypes);
        Vec<?>[] vecs = (Vec<?>[]) result;

        for (int i = 0; i < vecs[0].size(); i++) {
            System.out.println("block0 after omni:" + vecs[0].get(i));
            System.out.println("block1 after omni:" + vecs[1].get(i));
        }

        long end1 = System.currentTimeMillis();
        System.out.println("omni execute time: " + (end1 - start1));
        finished = true;
        return true;
    }

    @Override
    public O getResult()
    {
        checkState(finished, "process has not finished");
        return result;
    }
}
