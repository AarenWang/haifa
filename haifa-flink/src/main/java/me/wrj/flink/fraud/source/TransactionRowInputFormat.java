/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.wrj.flink.fraud.source;

import me.wrj.flink.fraud.entity.Transaction;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.io.GenericInputFormat;
import org.apache.flink.api.common.io.NonParallelInput;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.types.Row;

import java.sql.Timestamp;
import java.util.Iterator;

/** An bounded input of transactions. */
@Internal
public class TransactionRowInputFormat extends GenericInputFormat<Row> implements NonParallelInput {

    private static final long serialVersionUID = 1L;

    private transient Iterator<Transaction> transactions;

    @Override
    public void open(GenericInputSplit split) {
        transactions = TransactionIterator.bounded();
    }

    @Override
    public boolean reachedEnd() {
        return !transactions.hasNext();
    }

    @Override
    public Row nextRecord(Row reuse) {
        Transaction transaction = transactions.next();
        reuse.setField(0, transaction.getAccountId());
        reuse.setField(1, new Timestamp(transaction.getTimestamp()));
        reuse.setField(2, transaction.getAmount());

        return reuse;
    }
}
