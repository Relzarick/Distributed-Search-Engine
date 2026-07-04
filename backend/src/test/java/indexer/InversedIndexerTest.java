package indexer;

import db.Cache;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import tokenizer.StandardTokenizationV2;

import java.util.ArrayList;
import java.util.List;

class InversedIndexerTest {
    private InversedIndexer indexer;

    @Mock
    Cache cache;

    @BeforeEach
    void setUp() {
        indexer = new InversedIndexer(cache, new StandardTokenizationV2());
    }

    @Test
    void tokenizeToIndexTestOutput() {
        indexer.tokenizeToIndex(testData());
    }

    List<Document> testData() {
        List<Document> docs = new ArrayList<>();

        // 1. Ariel
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff781"))
                .append("id", 2)
                .append("title", "Ariel")
                .append("vote_average", 7.106)
                .append("vote_count", 371)
                .append("status", "Released")
                .append("release_date", "1988-10-21")
                .append("revenue", 0)
                .append("runtime", 73)
                .append("budget", 0)
                .append("imdb_id", "tt0094675")
                .append("original_language", "fi")
                .append("original_title", "Ariel")
                .append("overview", "A Finnish man goes to the city to find a job after the mine where he worked is closed and his father commits suicide.")
                .append("popularity", 1.6384)
                .append("tagline", "")
                .append("genres", "Comedy, Drama, Romance, Crime")
                .append("production_companies", "Villealfa Filmproductions")
                .append("production_countries", "Finland")
                .append("spoken_languages", "suomi")
                .append("cast", "Kari Helaseppä, Jaakko Talaskivi, Mikko Remes, Merja Pulkkinen, Esko Salminen, Timo Markko, Sami Lanki, Marja Packalén, Tarja Keinänen, Olli Varja, Matti Jaaranen, Heikki Anttila, Markku Rantala, Hannu Viholainen, Tomi Salmela, Kauko Laalo, Esko Nikkari, Hannu Kivisalo, Sirkka Rautiainen, Eetu Hilkamo, Jyrki Olsonen, Heikki Salomaa, Veikko Uusimäki, Turo Pajala, Timo Harakka, Juuso Hirvikangas, Jouko Lumme, Matti Pellonpää, Sakari Kuosmanen, Pentti Auer, Reijo Marin, Timo Toikka, Jorma Markkula, Mikko Lyytikäinen, Hanna Jokinen, Eino Kuusela, Pekka Wilen, Susanna Haavisto, Erkki Pajala")
                .append("director", "Aki Kaurismäki")
                .append("director_of_photography", "Timo Salminen")
                .append("writers", "Aki Kaurismäki")
                .append("producers", "Aki Kaurismäki")
                .append("music_composer", "")
                .append("imdb_rating", 7.4)
                .append("imdb_votes", 9809)
                .append("poster_path", "/ojDg0PGvs6R9xYFodRct2kdI6wC.jpg")
        );

        // 2. Shadows in Paradise
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff782"))
                .append("id", 3)
                .append("title", "Shadows in Paradise")
                .append("vote_average", 7.3)
                .append("vote_count", 439)
                .append("status", "Released")
                .append("release_date", "1986-10-17")
                .append("revenue", 0)
                .append("runtime", 74)
                .append("budget", 0)
                .append("imdb_id", "tt0092149")
                .append("original_language", "fi")
                .append("original_title", "Varjoja paratiisissa")
                .append("overview", "Nikander, a rubbish collector and would-be entrepreneur, finds his plans for success dashed when his business associate dies. One evening, he meets Ilona, a down-on-her-luck cashier, in a local supermarket. Falteringly, a bond begins to develop between them.")
                .append("popularity", 1.7599)
                .append("tagline", "")
                .append("genres", "Comedy, Drama, Romance")
                .append("production_companies", "Villealfa Filmproductions")
                .append("production_countries", "Finland")
                .append("spoken_languages", "svenska, suomi, English")
                .append("cast", "Jukka-Pekka Palo, Svante Korkiakoski, Mari Rantasila, Tanja Talaskivi, Erkki Rissanen, Ulla Kuosmanen, Matti Pellonpää, Jukka Mäkinen, Pentti Koski, Mato Valtonen, Bertta Pellonpää, Eskil Mansikka, Antti Ortamo, Sirkka Silin, Malla Hukkanen, Sakari Kuosmanen, Helmeri Pellonpää, Marina Martinoff, Ari Korhonen, Kati Outinen, Haije Alanoja, Neka Haapanen, Olli Varja, Riikka Kuosmanen, Esko Nikkari, Jussi Tiitinen, Aki Kaurismäki, Kylli Köngäs, Jaakko Talaskivi, Safka Pekkonen, Sakke Järvenpää, Pekka Laiho, Teuvo Rissanen")
                .append("director", "Aki Kaurismäki")
                .append("director_of_photography", "Timo Salminen")
                .append("writers", "Aki Kaurismäki")
                .append("producers", "Mika Kaurismäki")
                .append("music_composer", "")
                .append("imdb_rating", 7.4)
                .append("imdb_votes", 8676)
                .append("poster_path", "/nj01hspawPof0mJmlgfjuLyJuRN.jpg")
        );

        // 3. Four Rooms
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff783"))
                .append("id", 5)
                .append("title", "Four Rooms")
                .append("vote_average", 5.905)
                .append("vote_count", 2851)
                .append("status", "Released")
                .append("release_date", "1995-12-09")
                .append("revenue", 4257354)
                .append("runtime", 98)
                .append("budget", 4000000)
                .append("imdb_id", "tt0113101")
                .append("original_language", "en")
                .append("original_title", "Four Rooms")
                .append("overview", "It's Ted the Bellhop's first night on the job...and the hotel's very unusual guests are about to place him in some outrageous predicaments. It seems that this evening's room service is serving up one unbelievable happening after another.")
                .append("popularity", 3.6449)
                .append("tagline", "Twelve outrageous guests. Four scandalous requests. And one lone bellhop, in his first day on the job, who's in for the wildest New year's Eve of his life.")
                .append("genres", "Comedy")
                .append("production_companies", "Miramax, A Band Apart")
                .append("production_countries", "United States of America")
                .append("spoken_languages", "English")
                .append("cast", "Sammi Davis, Salma Hayek Pinault, Lawrence Bender, Madonna, Jennifer Beals, Alicia Witt, Lili Taylor, Patricia Vonne, Laura Rush, Unruly Julie McClean, Quentin Tarantino, Danny Verduzco, Tamlyn Tomita, Ione Skye, Marisa Tomei, Amanda de Cadenet, Lana McKissack, Tim Roth, Kimberly Blair, Marc Lawrence, Paul Skemp, Antonio Banderas, Kathy Griffin, Bruce Willis, David Proval, Quinn Hellerman, Valeria Golino, Paul Calderon")
                .append("director", "Quentin Tarantino, Robert Rodriguez, Alexandre Rockwell, Allison Anders")
                .append("director_of_photography", "Phil Parmet, Guillermo Navarro, Rodrigo García, Andrzej Sekula")
                .append("writers", "Quentin Tarantino, Robert Rodriguez, Alexandre Rockwell, Allison Anders")
                .append("producers", "Quentin Tarantino, Lawrence Bender, Alexandre Rockwell")
                .append("music_composer", "Combustible Edison")
                .append("imdb_rating", 6.7)
                .append("imdb_votes", 116998)
                .append("poster_path", "/75aHn1NOYXh4M7L5shoeQ6NGykP.jpg")
        );

        // 4. Judgment Night
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff784"))
                .append("id", 6)
                .append("title", "Judgment Night")
                .append("vote_average", 6.5)
                .append("vote_count", 370)
                .append("status", "Released")
                .append("release_date", "1993-10-15")
                .append("revenue", 12136938)
                .append("runtime", 109)
                .append("budget", 21000000)
                .append("imdb_id", "tt0107286")
                .append("original_language", "en")
                .append("original_title", "Judgment Night")
                .append("overview", "Four young friends, while taking a shortcut en route to a local boxing match, witness a brutal murder which leaves them running for their lives.")
                .append("popularity", 2.1596)
                .append("tagline", "Don't move. Don't whisper. Don't even breathe.")
                .append("genres", "Action, Crime, Thriller")
                .append("production_companies", "Largo Entertainment, JVC, Universal Pictures")
                .append("production_countries", "United States of America")
                .append("spoken_languages", "English")
                .append("cast", "Jeremy Piven, Lydell M. Cheshier, Michael DeLorenzo, Raichle Watt, Denis Leary, Christine Harnos, Will Zahrn, Galyn Görg, Emilio Estevez, Mark Phelan, Nigel Gibbs, Robert S. Neville, Lauren Robinson, Everlast, Sean O'Grady, Hank McGill, Cuba Gooding Jr., Michael Scranton, Doug Wert, Michael Wiseman, Kathleen Perkins, Stephen Dorff, Stuart Abramson, Angela Alvarado, David L. Crowley, Donovan D. Ross, Relioues Webb, Darin Mangan, Deirdre Kelly, Peter Greene, Eugene Williams")
                .append("director", "Stephen Hopkins")
                .append("director_of_photography", "Peter Levy")
                .append("writers", "Lewis Colick, Jere Cunningham")
                .append("producers", "Lloyd Segan, Gene Levy, Marilyn Vance")
                .append("music_composer", "Alan Silvestri")
                .append("imdb_rating", 6.6)
                .append("imdb_votes", 21114)
                .append("poster_path", "/3rvvpS9YPM5HB2f4HYiNiJVtdam.jpg")
        );

        // 5. Life in Loops (A Megacities RMX)
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff785"))
                .append("id", 8)
                .append("title", "Life in Loops (A Megacities RMX)")
                .append("vote_average", 7.2)
                .append("vote_count", 30)
                .append("status", "Released")
                .append("release_date", "2006-01-01")
                .append("revenue", 0)
                .append("runtime", 80)
                .append("budget", 42000)
                .append("imdb_id", "tt0825671")
                .append("original_language", "en")
                .append("original_title", "Life in Loops (A Megacities RMX)")
                .append("overview", "Timo Novotny labels his new project an experimental music documentary film, in a remix of the celebrated film Megacities (1997), a visually refined essay on the hidden faces of several world \"megacities\" by leading Austrian documentarist Michael Glawogger. Novotny complements 30 % of material taken straight from the film (and re-edited) with 70 % as yet unseen footage in which he blends original shots unused by Glawogger with his own sequences (shot by Megacities cameraman Wolfgang Thaler) from Tokyo. Alongside the Japanese metropolis, Life in Loops takes us right into the atmosphere of Mexico City, New York, Moscow and Bombay. This electrifying combination of fascinating film images and an equally compelling soundtrack from Sofa Surfers sets us off on a stunning audiovisual adventure across the continents. The film also makes an original contribution to the discussion on new trends in documentary filmmaking.")
                .append("popularity", 2.2585)
                .append("tagline", "A Megacities remix.")
                .append("genres", "Documentary")
                .append("production_companies", "inLoops")
                .append("production_countries", "Austria")
                .append("spoken_languages", "English, हिन्दी, 日本語, Pусский, Español")
                .append("cast", "")
                .append("director", "Timo Novotny")
                .append("director_of_photography", "Wolfgang Thaler")
                .append("writers", "Timo Novotny, Michael Glawogger")
                .append("producers", "Timo Novotny, Ulrich Gehmacher")
                .append("music_composer", "")
                .append("imdb_rating", 8.1)
                .append("imdb_votes", 285)
                .append("poster_path", "/7ln81BRnPR2wqxuITZxEciCe1lc.jpg")
        );

        // 6. Sunday in August
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff786"))
                .append("id", 9)
                .append("title", "Sunday in August")
                .append("vote_average", 6.8)
                .append("vote_count", 28)
                .append("status", "Released")
                .append("release_date", "2004-09-02")
                .append("revenue", 0)
                .append("runtime", 15)
                .append("budget", 0)
                .append("imdb_id", "tt0425473")
                .append("original_language", "de")
                .append("original_title", "Sonntag, im August")
                .append("overview", "A couple on a boat. Their love is burnt out. But how to let go when souls are entangled?")
                .append("popularity", 0.6529)
                .append("tagline", "")
                .append("genres", "Drama")
                .append("production_companies", "")
                .append("production_countries", "Germany")
                .append("spoken_languages", "Deutsch")
                .append("cast", "Milton Welsh, Rita Lengyel")
                .append("director", "Marc Meyer")
                .append("director_of_photography", "Peter Polsak-Lohmann")
                .append("writers", "Marc Meyer")
                .append("producers", "Marc Meyer")
                .append("music_composer", "Christian Biegai")
                .append("imdb_rating", 6.8)
                .append("imdb_votes", 14)
                .append("poster_path", "")
        );

        // 7. Star Wars
        docs.add(new Document()
                .append("_id", new ObjectId("6a476be4c3d3c7d749aff787"))
                .append("id", 11)
                .append("title", "Star Wars")
                .append("vote_average", 8.205)
                .append("vote_count", 22400)
                .append("status", "Released")
                .append("release_date", "1977-05-25")
                .append("revenue", 775398007)
                .append("runtime", 121)
                .append("budget", 11000000)
                .append("imdb_id", "tt0076759")
                .append("original_language", "en")
                .append("original_title", "Star Wars")
                .append("overview", "Princess Leia is captured and held hostage by the evil Imperial forces in their effort to take over the galactic Empire. Venturesome Luke Skywalker and dashing captain Han Solo team together with the loveable robot duo R2-D2 and C-3PO to rescue the beautiful princess and restore peace and justice in the Empire.")
                .append("popularity", 21.0617)
                .append("tagline", "A long time ago in a galaxy far, far away...")
                .append("genres", "Adventure, Action, Science Fiction")
                .append("production_companies", "Lucasfilm Ltd.")
                .append("production_countries", "United States of America")
                .append("spoken_languages", "English")
                .append("cast", "Paul Blake, Carrie Fisher, Steve Gawley, Rick McCallum, Jack Klaff, Peter Sumner, Eddie Eddon, James Earl Jones, David Prowse, Erica Simmons, Grant McCune, Janice Burchette, Steve Williams, George Roubicek, Pam Rose, Shane Rimmer, Ted Gagliano, Alan Harris, Burnell Tucker, Peter Sturgeon, Anthony Lang, Larry Ward, Michael Leader, Isaac Grand, Barry Gnome, Linda Jones, Harry Fielder, Barry Copping, Peter Mayhew, Tommy Ilsley, Ted Burnett, Joe Johnston, Malcolm Tierney, Derek Lyons, Jerry Walter, Anthony Forrest, Angus MacInnes, Mahjoub, Mark Hamill, George Stock, Melissa Kurtz, Reg Harding, Alf Mangan, Garrick Hagon, Phil Brown, Alex McCrindle, Leslie Schofield, Tiffany Hillkurtz, Eddie Byrne, Don Henderson, Gilda Cohen, Bill Weston, John Sylla, Jon Berg, Salo Gardner, Peter Diamond, Warwick Diamond, Doug Beswick, Scott Beach, Joe Kaye, Tom Sylla, Geoffrey Moon, Jeremy Sinden, Drewe Henley, Kenny Baker, Frank Henson, Diana Sadley Way, Shelagh Fraser, Roy Straite, Fred Wood, John Cannon, Marcus Powell, Peter Cushing, Morgan Upton, Sadie Eden, Ron Tarr, Lorne Peterson, Robert A. Denham, Denis Lawson, Maria De Aragon, Kim Falkinburg, Al Lampert, Richard LeParmentier, Hal Wamsley, William Hootkins, Angela Staines, Harrison Ford, John Chapman, Jack Purvis, Arthur Howell, Robert Davies, Lightning Bear, Anthony Daniels, Mandy Morton, Graham Ashley, Tim Condren, Alec Guinness, Colin Higgins, Frazer Diamond, Rusty Goffe, Phil Tippett, Colin Michael Kitchens, David Ankrum, Alfie Curtis")
                .append("director", "George Lucas")
                .append("director_of_photography", "Gilbert Taylor")
                .append("writers", "George Lucas")
                .append("producers", "George Lucas, Gary Kurtz")
                .append("music_composer", "John Williams")
                .append("imdb_rating", 8.6)
                .append("imdb_votes", 1573502)
                .append("poster_path", "/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg")
        );

        return docs;
    }
}