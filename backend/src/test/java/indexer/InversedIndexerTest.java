package indexer;

import etl.QueueItem;
import indexer.tokenizer.StandardTokenizationV3;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@ExtendWith(MockitoExtension.class)
class InversedIndexerTest {
    private InversedIndexer indexer;
    private final BlockingQueue<QueueItem> indexerQueue = new ArrayBlockingQueue<>(100);

    @BeforeEach
    void setUp() {
        indexer = new InversedIndexer(new StandardTokenizationV3());
    }

    @Test
    void tokenizeToIndexTestOutput() throws InterruptedException {
        indexer.tokenizeToQueue(testData(), indexerQueue);
    }

    List<Document> testData() {
        List<Document> docs = new ArrayList<>();

        // 1. Birth of a Monster
        docs.add(new Document()
                .append("_id", "019f72bf-c76f-75d1-8119-12394b6d413c")
                .append("id", 852962)
                .append("title", "Birth of a Monster")
                .append("vote_average", 0.0)
                .append("vote_count", 0.0)
                .append("status", "Released")
                .append("release_date", "2017-01-01")
                .append("revenue", 0.0)
                .append("runtime", 53.0)
                .append("budget", 0.0)
                .append("imdb_id", "")
                .append("original_language", "en")
                .append("original_title", "Fabrication d'un monstre")
                .append("overview", "For the past 12 years, journalist Paul Moreira has travelled extensively in Iraq. In this film, he goes in search of the men he filmed back in 2003 at the very beginning of the American occupation. Through their stories, and by tracing the roots of ISIS to the arrival of Abu Mousab Al-Zarqawi and America's handling of the resistance, he tells the story of how Iraq became such a fractured nation.")
                .append("popularity", 0.129)
                .append("tagline", "")
                .append("genres", "Documentary, War")
                .append("production_companies", "")
                .append("production_countries", "")
                .append("spoken_languages", "English")
                .append("cast", "")
                .append("director", "Paul Moreira")
                .append("director_of_photography", "")
                .append("writers", "")
                .append("producers", "Luc Hermann")
                .append("music_composer", "")
                .append("imdb_rating", null)
                .append("imdb_votes", null)
                .append("poster_path", "/haLQouAukGOWs113RogKU56d77y.jpg")
        );

        // 2. Chhota Bheem: The Crown of Valhalla
        docs.add(new Document()
                .append("_id", "019f72bf-c770-755e-8cfd-4357fc0bf375")
                .append("id", 852963)
                .append("title", "Chhota Bheem: The Crown of Valhalla")
                .append("vote_average", 10.0)
                .append("vote_count", 1.0)
                .append("status", "Released")
                .append("release_date", "2013-05-01")
                .append("revenue", 0.0)
                .append("runtime", 69.0)
                .append("budget", 0.0)
                .append("imdb_id", "tt6442766")
                .append("original_language", "hi")
                .append("original_title", "Chhota Bheem and the Crown of Valhalla")
                .append("overview", "To save the kingdom of Valhalla from invasion by Vikings, Bheem is gathering his army of friends, who are ready to put up a formidable fight!")
                .append("popularity", 0.4716)
                .append("tagline", "")
                .append("genres", "Animation")
                .append("production_companies", "Green Gold Animation")
                .append("production_countries", "India")
                .append("spoken_languages", "हिन्दी")
                .append("cast", "Vatsal Dubey, Rajesh Kava, Julie Tejwani, Jigna Bhardwaj, Rupa Bhimani")
                .append("director", "Rajiv Chilaka")
                .append("director_of_photography", "")
                .append("writers", "")
                .append("producers", "")
                .append("music_composer", "")
                .append("imdb_rating", 8.1)
                .append("imdb_votes", 91.0)
                .append("poster_path", "/rAnp8d4kCKpec7bScxC2m6DHujo.jpg")
        );

        // 3. Nezlob, Kristino
        docs.add(new Document()
                .append("_id", "019f72bf-c770-755e-8cfd-4357fc0bf376")
                .append("id", 852964)
                .append("title", "Nezlob, Kristino")
                .append("vote_average", 4.0)
                .append("vote_count", 1.0)
                .append("status", "Released")
                .append("release_date", "1956-08-10")
                .append("revenue", 0.0)
                .append("runtime", 0.0)
                .append("budget", 0.0)
                .append("imdb_id", "tt0247576")
                .append("original_language", "cs")
                .append("original_title", "Nezlob, Kristino")
                .append("overview", "")
                .append("popularity", 0.0404)
                .append("tagline", "")
                .append("genres", "Comedy")
                .append("production_companies", "Studio hraných filmů")
                .append("production_countries", "Czechoslovakia")
                .append("spoken_languages", "Český")
                .append("cast", "Svatopluk Beneš, Antonín Rýdl, Vladimír Pucholt, Jiří Němeček, Elena Hálková, Gabriela Bártlová-Buddeusová, Jiří Steimar, Antonín Hardt, František Filipovský, Václav Postránecký, Ludmila Vendlová, Zdeňka Baldová, Jiří Dohnal, Miloš Nesvadba, Karel Höger, Bohuš Záhorský, Drahomíra Fialková, Miloš Nedbal, Miloš Vavruška, Zdenka Procházková, Rudolf Deyl")
                .append("director", "Vladimír Čech")
                .append("director_of_photography", "Rudolf Stahl")
                .append("writers", "Vladimír Neff, Vlasta Petrovičová")
                .append("producers", "")
                .append("music_composer", "Dalibor C. Vačkář")
                .append("imdb_rating", 4.7)
                .append("imdb_votes", 13.0)
                .append("poster_path", "")
        );

        // 4. Chhota Bheem and the Shinobi Secret
        docs.add(new Document()
                .append("_id", "019f72bf-c770-755e-8cfd-4357fc0bf377")
                .append("id", 852965)
                .append("title", "Chhota Bheem and the Shinobi Secret")
                .append("vote_average", 0.0)
                .append("vote_count", 0.0)
                .append("status", "Released")
                .append("release_date", "2013-11-03")
                .append("revenue", 0.0)
                .append("runtime", 63.0)
                .append("budget", 0.0)
                .append("imdb_id", "tt6417830")
                .append("original_language", "hi")
                .append("original_title", "Chhota Bheem and the Shinobi Secret")
                .append("overview", "After learning of a samurai village under threat by their own emperor, Bheem sets off for Japan to offer his help.")
                .append("popularity", 0.2885)
                .append("tagline", "")
                .append("genres", "Adventure, Animation, Comedy")
                .append("production_companies", "")
                .append("production_countries", "India")
                .append("spoken_languages", "")
                .append("cast", "")
                .append("director", "Rajiv Chilaka")
                .append("director_of_photography", "")
                .append("writers", "Darsana Radhakrishnan")
                .append("producers", "")
                .append("music_composer", "")
                .append("imdb_rating", 7.9)
                .append("imdb_votes", 87.0)
                .append("poster_path", "/fmt05bmw014lPlZEkSztupQW3uI.jpg")
        );

        // 5. Stories of the Subconscious Mind
        docs.add(new Document()
                .append("_id", "019f72bf-c770-755e-8cfd-4357fc0bf378")
                .append("id", 852966)
                .append("title", "Stories of the Subconscious Mind")
                .append("vote_average", 6.0)
                .append("vote_count", 1.0)
                .append("status", "Released")
                .append("release_date", "2018-05-10")
                .append("revenue", 0.0)
                .append("runtime", 10.0)
                .append("budget", 0.0)
                .append("imdb_id", "tt8244842")
                .append("original_language", "en")
                .append("original_title", "Stories of the Subconscious Mind")
                .append("overview", "Psychiatrist Alice Davenport has the unique ability to enter people's subconscious minds. When Carter Brooks, a suicidal young man, enters her office, she must go inside his head to fight his inner demon before it kills him.")
                .append("popularity", 0.6)
                .append("tagline", "")
                .append("genres", "Horror")
                .append("production_companies", "")
                .append("production_countries", "United Kingdom")
                .append("spoken_languages", "English")
                .append("cast", "George Nettleton, Bethan Nash")
                .append("director", "Curt Dennis")
                .append("director_of_photography", "")
                .append("writers", "Curt Dennis")
                .append("producers", "Scott Dance, Max Mir")
                .append("music_composer", "")
                .append("imdb_rating", 7.0)
                .append("imdb_votes", 36.0)
                .append("poster_path", "/uemRTRhoLewcWOXxqvKBA6dnv7B.jpg")
        );

        return docs;
    }
}