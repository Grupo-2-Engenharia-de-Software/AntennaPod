package de.danoeh.antennapod.storage.importexport.repository;

import android.content.Context;
import android.net.Uri;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.importexport.OpmlElement;
import de.danoeh.antennapod.storage.importexport.OpmlReader;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

public class OpmlRepository {

    private final Context context;

    public OpmlRepository(Context context) {
        this.context = context;
    }

    /**
     * Le um arquivo OPM e retorna uma lista de opmfiles
     * Operação executada em uma thread no background (I/O)
     */
    public Single<List<OpmlElement>> readOpmlFile(Uri uri) {
        return Single.fromCallable(() -> {
            InputStream opmlFileStream = context.getContentResolver().openInputStream(uri);
            BOMInputStream bomInputStream = new BOMInputStream(opmlFileStream);
            ByteOrderMark bom = bomInputStream.getBOM();
            String charsetName = (bom == null) ? "UTF-8" : bom.getCharsetName();
            Reader reader = new InputStreamReader(bomInputStream, charsetName);
            OpmlReader opmlReader = new OpmlReader();
            List<OpmlElement> result = opmlReader.readDocument(reader);
            reader.close();
            return result;
        });
    }

    /**
     * Importa os feeds selecionados no database
     * Operação executada em uma thread no background (I/O)
     */
    public Completable importSelectedFeeds(List<OpmlElement> selectedElements) {
        return Completable.fromAction(() -> {
            for (OpmlElement element : selectedElements) {
                Feed feed = new Feed(element.getXmlUrl(), null,
                        element.getText() != null ? element.getText() : "Unknown podcast");
                feed.setItems(Collections.emptyList());
                FeedDatabaseWriter.updateFeed(context, feed, false);
            }
        }).andThen(Completable.fromAction(() -> {
            FeedUpdateManager.getInstance().runOnce(context);
        }));
    }
}