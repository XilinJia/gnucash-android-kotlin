/*
 * Copyright (c) 2018 Semyannikov Gleb <nightdevgame@gmail.com>
 *
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

package org.gnucash.android.export.csv;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.R;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates a GnuCash CSV transactions representation of the accounts and transactions
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvTransactionsExporter extends Exporter{

    private char mCsvSeparator;

    private DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd", Locale.US);

    /**
     * Construct a new exporter with export parameters
     * @param params Parameters for the export
     */
    public CsvTransactionsExporter(ExportParams params) {
        super(params, null);
        mCsvSeparator = params.getCsvSeparator();
        LOG_TAG = "GncXmlExporter";
    }

    /**
     * Overloaded constructor.
     * Creates an exporter with an already open database instance.
     * @param params Parameters for the export
     * @param db SQLite database
     */
    public CsvTransactionsExporter(ExportParams params, SQLiteDatabase db) {
        super(params, db);
        mCsvSeparator = params.getCsvSeparator();
        LOG_TAG = "GncXmlExporter";
    }

    @Override
    public List<String> generateExport() throws ExporterException {
        String outputFile = getExportCacheFilePath();

        try (CsvWriter csvWriter = new CsvWriter(new FileWriter(outputFile), "" + mCsvSeparator)){
            generateExport(csvWriter);
        } catch (IOException ex){
            Crashlytics.log("Error exporting CSV");
            Crashlytics.logException(ex);
            throw new ExporterException(mExportParams, ex);
        }

        return Arrays.asList(outputFile);
    }

    /**
     * Write splits to CSV format
     * @param splits Splits to be written
     */
    private void writeSplitsToCsv(@NonNull List<Split> splits, @NonNull CsvWriter writer) throws IOException {
        int index = 0;

        Map<String, Account> uidAccountMap = new HashMap<>();

        for (Split split : splits) {
            if (index++ > 0){ // the first split is on the same line as the transactions. But after that, we
                writer.write("" + mCsvSeparator + mCsvSeparator + mCsvSeparator + mCsvSeparator
                        + mCsvSeparator + mCsvSeparator + mCsvSeparator + mCsvSeparator);
            }
            writer.writeToken(split.getMMemo());

            //cache accounts so that we do not have to go to the DB each time
            String accountUID = split.getMAccountUID();
            Account account;
            if (uidAccountMap.containsKey(accountUID)) {
                account = uidAccountMap.get(accountUID);
            } else {
                account = mAccountsDbAdapter.getRecord(accountUID);
                uidAccountMap.put(accountUID, account);
            }

            writer.writeToken(account.getMFullName());
            writer.writeToken(account.getMName());

            String sign = split.getMSplitType() == TransactionType.CREDIT ? "-" : "";
            writer.writeToken(sign + split.getMQuantity().formattedString());
            writer.writeToken(sign + split.getMQuantity().toLocaleString());
            writer.writeToken("" + split.getMReconcileState());
            if (split.getMReconcileState() == Split.FLAG_RECONCILED) {
                String recDateString = dateFormat.format(new Date(split.getMReconcileDate().getTime()));
                writer.writeToken(recDateString);
            } else {
                writer.writeToken(null);
            }
            writer.writeEndToken(split.getMQuantity().divide(split.getMValue()).toLocaleString());
        }
    }

    private void generateExport(final CsvWriter csvWriter) throws ExporterException {
        try {
            List<String> names = Arrays.asList(mContext.getResources().getStringArray(R.array.csv_transaction_headers));
            for(int i = 0; i < names.size(); i++) {
                csvWriter.writeToken(names.get(i));
            }
            csvWriter.newLine();


            Cursor cursor = mTransactionsDbAdapter.fetchTransactionsModifiedSince(mExportParams.getExportStartTime());
            Log.d(LOG_TAG, String.format("Exporting %d transactions to CSV", cursor.getCount()));
            while (cursor.moveToNext()){
                Transaction transaction = mTransactionsDbAdapter.buildModelInstance(cursor);
                Date date = new Date(transaction.getMTimestamp());
                csvWriter.writeToken(dateFormat.format(date));
                csvWriter.writeToken(transaction.getMUID());
                csvWriter.writeToken(null);  //Transaction number

                csvWriter.writeToken(transaction.getMDescription());
                csvWriter.writeToken(transaction.getMNotes());

                csvWriter.writeToken("CURRENCY::" + transaction.getMMnemonic());
                csvWriter.writeToken(null); // Void Reason
                csvWriter.writeToken(null); // Action
                writeSplitsToCsv(transaction.getMSplitList(), csvWriter);
            }

            PreferencesHelper.setLastExportTime(TimestampHelper.getTimestampFromNow());
        } catch (IOException e) {
            Crashlytics.logException(e);
            throw new ExporterException(mExportParams, e);
        }
    }
}
