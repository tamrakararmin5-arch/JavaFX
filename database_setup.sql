SET SQL_SAFE_UPDATES = 0;
CREATE DATABASE IF NOT EXISTS dbconnect;
USE dbconnect;

CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) UNIQUE,
    author      VARCHAR(255),
    genre       VARCHAR(100),
    pages       INT,
    description TEXT
);

CREATE TABLE IF NOT EXISTS favourites (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    book_title VARCHAR(255) NOT NULL,
    UNIQUE KEY uq_fav (user_id, book_title),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    book_title  VARCHAR(255) NOT NULL,
    rating      INT NOT NULL,
    review_text TEXT,
    UNIQUE KEY uq_review (user_id, book_title),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
INSERT IGNORE INTO books (title, author, genre, pages, description) VALUES
('The Hobbit', 'J.R.R. Tolkien', 'Fantasy', 310, 'A peaceful hobbit embarks on a dangerous quest with a company of dwarves to reclaim their homeland from a fearsome dragon.'),
('The Fellowship of the Ring', 'J.R.R. Tolkien', 'Fantasy', 423, 'A young hobbit inherits a powerful ring and must journey to destroy it before a dark lord claims it.'),
('The Name of the Wind', 'Patrick Rothfuss', 'Fantasy', 662, 'A legendary wizard recounts his extraordinary life from orphaned street rat to the most feared man in the world.'),
('Harry Potter and the Sorcerers Stone', 'J.K. Rowling', 'Fantasy', 309, 'A young boy discovers on his 11th birthday that he is a famous wizard and attends a magical school.'),
('A Game of Thrones', 'George R.R. Martin', 'Fantasy', 694, 'Noble families war for control of the Iron Throne while an ancient threat awakens beyond the northern border.'),
('The Way of Kings', 'Brandon Sanderson', 'Fantasy', 1007, 'Three unlikely heroes are drawn into a world-altering conflict on a storm-ravaged world with a forgotten history.'),
('Mistborn: The Final Empire', 'Brandon Sanderson', 'Fantasy', 541, 'A street thief with rare powers joins a rebellion to overthrow an immortal ruler who controls ash and mist.'),
('The Lies of Locke Lamora', 'Scott Lynch', 'Fantasy', 499, 'A gang of brilliant thieves run elaborate cons in a fantasy city but find themselves in over their heads.'),
('Elantris', 'Brandon Sanderson', 'Fantasy', 358, 'A city of gods has fallen into ruin, and a prince works to uncover the secret that destroyed it.'),
('Eragon', 'Christopher Paolini', 'Fantasy', 503, 'A farm boy discovers a dragon egg and is thrust into an ancient conflict between riders and an evil king.'),
('The Alchemist', 'Paulo Coelho', 'Fantasy', 208, 'A shepherd boy travels from Spain to Egypt chasing a recurring dream and discovers the meaning of life.'),
('Jonathan Strange and Mr Norrell', 'Susanna Clarke', 'Fantasy', 782, 'Two English magicians revive magic in 19th-century England with disastrous consequences for both.'),
('The Blade Itself', 'Joe Abercrombie', 'Fantasy', 515, 'A crippled torturer, a barbarian warrior, and a renowned hero are drawn together by a sinister plot.'),
('The Color of Magic', 'Terry Pratchett', 'Fantasy', 243, 'A hapless wizard guides the Discworlds first tourist in a wildly comedic fantasy adventure.'),
('Good Omens', 'Terry Pratchett and Neil Gaiman', 'Fantasy', 288, 'An angel and a demon team up to prevent the apocalypse because they have both grown rather fond of Earth.'),
('American Gods', 'Neil Gaiman', 'Fantasy', 465, 'An ex-convict becomes entangled in a war between old gods and new gods across America.'),
('Coraline', 'Neil Gaiman', 'Fantasy', 162, 'A young girl discovers a secret door in her house leading to a parallel world with a sinister other mother.'),
('The Night Circus', 'Erin Morgenstern', 'Fantasy', 387, 'Two young magicians are pitted against each other in a mysterious competition inside a magical black-and-white circus.'),
('Assassins Apprentice', 'Robin Hobb', 'Fantasy', 356, 'The illegitimate son of a prince is trained as an assassin while uncovering a deadly conspiracy.'),
('The Stormlight Archive', 'Brandon Sanderson', 'Fantasy', 1258, 'An epic tale of knights, storms, and the ancient secrets of a world on the brink of catastrophe.'),
('Dune', 'Frank Herbert', 'Sci-Fi', 412, 'A desert planet holds the most valuable substance in the universe, and one family must survive its deadly politics.'),
('Foundation', 'Isaac Asimov', 'Sci-Fi', 255, 'A mathematician uses psychohistory to predict the fall of a galactic empire and tries to shorten the dark age that follows.'),
('Brave New World', 'Aldous Huxley', 'Sci-Fi', 268, 'A futuristic society engineered for happiness sacrifices freedom, family, and art for stability.'),
('Project Hail Mary', 'Andy Weir', 'Sci-Fi', 476, 'A lone astronaut wakes up millions of miles from Earth with no memory and the fate of humanity in his hands.'),
('The Martian', 'Andy Weir', 'Sci-Fi', 369, 'An astronaut is stranded alone on Mars and must use science and ingenuity to survive until rescue.'),
('Enders Game', 'Orson Scott Card', 'Sci-Fi', 352, 'A child genius is recruited to a military school in space to prepare humanity for an alien invasion.'),
('Neuromancer', 'William Gibson', 'Sci-Fi', 271, 'A washed-up hacker is hired for a mysterious job in a dystopian world of corporate espionage and AI.'),
('The Hitchhikers Guide to the Galaxy', 'Douglas Adams', 'Sci-Fi', 193, 'Moments before Earth is demolished, a man is whisked into space on a cosmic adventure.'),
('2001: A Space Odyssey', 'Arthur C. Clarke', 'Sci-Fi', 224, 'A crew of astronauts voyage to Jupiter guided by a dangerously intelligent AI named HAL.'),
('Rendezvous with Rama', 'Arthur C. Clarke', 'Sci-Fi', 243, 'Astronauts explore a mysterious alien starship that passes through the solar system.'),
('Hyperion', 'Dan Simmons', 'Sci-Fi', 482, 'Seven pilgrims journey to a deadly creature, each carrying a story that may save or doom humanity.'),
('The Left Hand of Darkness', 'Ursula K. Le Guin', 'Sci-Fi', 286, 'A human envoy visits a planet whose inhabitants have no fixed gender, exploring politics and identity.'),
('Snow Crash', 'Neal Stephenson', 'Sci-Fi', 440, 'A hacker uncovers a conspiracy linking an ancient Sumerian language to a dangerous computer virus.'),
('Do Androids Dream of Electric Sheep', 'Philip K. Dick', 'Sci-Fi', 210, 'A bounty hunter tracks rogue androids in a post-nuclear world and questions the nature of humanity.'),
('The Time Machine', 'H.G. Wells', 'Sci-Fi', 118, 'A scientist travels to the distant future and discovers humanity has evolved into two very different species.'),
('Old Mans War', 'John Scalzi', 'Sci-Fi', 351, 'A 75-year-old farmer enlists in an interstellar military and receives a new young body to fight alien wars.'),
('Ready Player One', 'Ernest Cline', 'Sci-Fi', 374, 'In a bleak future, humanity escapes into virtual reality where one teen hunts for a hidden fortune.'),
('Leviathan Wakes', 'James S.A. Corey', 'Sci-Fi', 561, 'A spaceship detective and colonial officer uncover a conspiracy that threatens war across the solar system.'),
('Children of Time', 'Adrian Tchaikovsky', 'Sci-Fi', 600, 'Uplifted spiders and the last remnants of humanity converge on a distant world in this evolutionary epic.'),
('Enders Shadow', 'Orson Scott Card', 'Sci-Fi', 379, 'The events of Enders Game retold from the perspective of Bean, a brilliant orphan in the same battle school.'),
('Murder on the Orient Express', 'Agatha Christie', 'Mystery', 256, 'Detective Hercule Poirot investigates a murder on a snowbound train where every passenger is a suspect.'),
('The Hound of the Baskervilles', 'Arthur Conan Doyle', 'Mystery', 256, 'Holmes investigates an ancient curse that seems to be killing off the heirs of a wealthy English family.'),
('Gone Girl', 'Gillian Flynn', 'Mystery', 422, 'When a woman disappears on her anniversary, her husband becomes the prime suspect in a media frenzy.'),
('The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Mystery', 465, 'A journalist and a hacker investigate a decades-old disappearance tied to a powerful Swedish family.'),
('In the Woods', 'Tana French', 'Mystery', 429, 'A detective investigates a murder near the woods where two of his childhood friends vanished.'),
('The Big Sleep', 'Raymond Chandler', 'Mystery', 231, 'Private detective Philip Marlowe is hired by a dying general and uncovers a web of blackmail and murder.'),
('And Then There Were None', 'Agatha Christie', 'Mystery', 264, 'Ten strangers lured to an isolated island begin dying one by one according to a sinister nursery rhyme.'),
('The Da Vinci Code', 'Dan Brown', 'Mystery', 454, 'A symbologist unravels clues hidden in Da Vincis works pointing to a massive historical conspiracy.'),
('Big Little Lies', 'Liane Moriarty', 'Mystery', 460, 'Three women in a seaside town are connected by a dark secret that ends in murder.'),
('The No. 1 Ladies Detective Agency', 'Alexander McCall Smith', 'Mystery', 235, 'Botswanas first female detective solves everyday mysteries with wisdom, warmth, and a love of tea.'),
('The Silent Patient', 'Alex Michaelides', 'Thriller', 336, 'A famous painter shoots her husband five times then never speaks again. A therapist is obsessed with finding out why.'),
('The Girl on the Train', 'Paula Hawkins', 'Thriller', 395, 'An alcoholic divorcee becomes entangled in a missing person investigation she may know more about than she realises.'),
('Behind Closed Doors', 'B.A. Paris', 'Thriller', 294, 'A seemingly perfect marriage hides a terrifying secret that the wife is desperate to escape.'),
('The Woman in the Window', 'A.J. Finn', 'Thriller', 427, 'An agoraphobic woman believes she witnesses a murder through her neighbours window, but no one believes her.'),
('I Am Pilgrim', 'Terry Hayes', 'Thriller', 608, 'A retired US intelligence agent is drawn back into action to stop a bioterrorist from unleashing a deadly plague.'),
('The Firm', 'John Grisham', 'Thriller', 421, 'A promising young lawyer joins a prestigious firm only to discover it is a front for the mafia.'),
('The Pelican Brief', 'John Grisham', 'Thriller', 422, 'A law student uncovers the reason behind two Supreme Court murders and finds herself a target.'),
('Inferno', 'Dan Brown', 'Thriller', 461, 'Robert Langdon wakes with amnesia in Florence and must decode clues from Dantes Inferno to prevent a global plague.'),
('The Bourne Identity', 'Robert Ludlum', 'Thriller', 523, 'A man pulled from the sea with no memory but extraordinary combat skills finds the world determined to kill him.'),
('Along Came a Spider', 'James Patterson', 'Thriller', 434, 'Detective Alex Cross hunts a brilliant kidnapper who has taken two high-profile children from a private school.'),
('Dracula', 'Bram Stoker', 'Horror', 418, 'An English solicitor travels to Transylvania and unknowingly helps a vampire move to London to prey on the living.'),
('The Shining', 'Stephen King', 'Horror', 447, 'A struggling writer takes a job as winter caretaker at an isolated hotel, with terrifying consequences for his family.'),
('It', 'Stephen King', 'Horror', 1138, 'Seven childhood friends face an ancient evil lurking beneath their town, first as children and then as adults.'),
('Frankenstein', 'Mary Shelley', 'Horror', 280, 'A young scientist creates a living being from dead tissue, then abandons it with catastrophic results.'),
('Pet Sematary', 'Stephen King', 'Horror', 374, 'A doctor discovers a burial ground near his new home that can bring the dead back to life, but not as they were.'),
('House of Leaves', 'Mark Z. Danielewski', 'Horror', 709, 'A family discovers their house is slightly bigger on the inside than outside, and what lurks within is incomprehensible.'),
('The Haunting of Hill House', 'Shirley Jackson', 'Horror', 246, 'Four people gather in a notoriously haunted house to investigate the paranormal with devastating psychological effects.'),
('Bird Box', 'Josh Malerman', 'Horror', 262, 'Mysterious creatures have driven most of humanity insane. The only safety is to keep your eyes shut.'),
('1984', 'George Orwell', 'Classic', 328, 'In a totalitarian future, a government worker begins to question the Party and falls in love with a dangerous woman.'),
('The Great Gatsby', 'F. Scott Fitzgerald', 'Classic', 180, 'A mysterious millionaire throws lavish parties hoping to win back the woman he loves in 1920s Long Island.'),
('Pride and Prejudice', 'Jane Austen', 'Classic', 432, 'The witty Elizabeth Bennet navigates love, class, and family expectations in Regency England.'),
('To Kill a Mockingbird', 'Harper Lee', 'Classic', 281, 'A lawyer in the American South defends a Black man falsely accused of a crime, seen through his daughters eyes.'),
('The Catcher in the Rye', 'J.D. Salinger', 'Classic', 277, 'A rebellious teenager is expelled from prep school and drifts through New York City over three days.'),
('Jane Eyre', 'Charlotte Bronte', 'Classic', 500, 'An orphaned girl becomes a governess and falls in love with her brooding employer who harbours a dark secret.'),
('Crime and Punishment', 'Fyodor Dostoevsky', 'Classic', 545, 'A student commits murder convinced he is above ordinary morality, and is slowly consumed by guilt and paranoia.'),
('Moby Dick', 'Herman Melville', 'Classic', 635, 'Captain Ahabs monomaniacal quest for the white whale that took his leg drives his crew toward destruction.'),
('The Hunger Games', 'Suzanne Collins', 'Dystopian', 374, 'In a ruined North America, a teenager volunteers to fight in a televised death match to save her younger sister.'),
('Divergent', 'Veronica Roth', 'Dystopian', 487, 'A girl in a divided future society discovers she does not fit into any single faction and uncovers a sinister conspiracy.'),
('The Maze Runner', 'James Dashner', 'Dystopian', 374, 'A boy wakes up with no memory in a glade surrounded by a deadly moving maze and must find a way out.'),
('The Giver', 'Lois Lowry', 'Dystopian', 179, 'A boy in a seemingly perfect society is chosen to hold all of humanitys memories and learns a terrible truth.'),
('Fahrenheit 451', 'Ray Bradbury', 'Dystopian', 256, 'A fireman whose job is to burn books begins to question the society that banned them.'),
('The Handmaids Tale', 'Margaret Atwood', 'Dystopian', 311, 'In a theocratic future America, a woman is forced to serve as a reproductive surrogate and plots her escape.'),
('Atomic Habits', 'James Clear', 'Self-Help', 320, 'A framework for building good habits and breaking bad ones through small, consistent 1 percent improvements each day.'),
('The 7 Habits of Highly Effective People', 'Stephen R. Covey', 'Self-Help', 381, 'A principle-centred approach to personal and professional effectiveness through seven foundational habits.'),
('Think and Grow Rich', 'Napoleon Hill', 'Self-Help', 238, 'Drawn from interviews with 500 wealthy Americans, this book outlines 13 principles for achieving success.'),
('How to Win Friends and Influence People', 'Dale Carnegie', 'Self-Help', 288, 'Timeless advice on handling people, making friends, and winning others to your way of thinking.'),
('Deep Work', 'Cal Newport', 'Self-Help', 296, 'The case for focused, distraction-free work as the key skill of the modern economy and how to cultivate it.'),
('The Power of Now', 'Eckhart Tolle', 'Self-Help', 236, 'A guide to spiritual enlightenment through living fully in the present moment and releasing mental noise.'),
('Cant Hurt Me', 'David Goggins', 'Self-Help', 364, 'A Navy SEALs memoir of overcoming an abusive childhood and extraordinary challenges through mental toughness.'),
('Mindset', 'Carol S. Dweck', 'Self-Help', 277, 'Research showing how a growth mindset leads to greater achievement and resilience in all areas of life.'),
('Sapiens', 'Yuval Noah Harari', 'Non-Fiction', 443, 'A history of humankind from the Stone Age to the 21st century, exploring how Homo sapiens came to dominate Earth.'),
('Homo Deus', 'Yuval Noah Harari', 'Non-Fiction', 450, 'An exploration of humanitys future as we pursue happiness, immortality, and the power of gods through technology.'),
('Educated', 'Tara Westover', 'Non-Fiction', 334, 'A woman raised in a survivalist family in the mountains of Idaho educates herself and escapes to Cambridge.'),
('Becoming', 'Michelle Obama', 'Non-Fiction', 426, 'The memoir of the former First Lady, tracing her journey from Chicagos South Side to the White House.'),
('The Immortal Life of Henrietta Lacks', 'Rebecca Skloot', 'Non-Fiction', 381, 'The story of a Black woman whose cancer cells were taken without consent and became one of medicines most important tools.'),
('Thinking Fast and Slow', 'Daniel Kahneman', 'Non-Fiction', 499, 'A Nobel laureate explores the two systems of thought that drive our decisions and the biases that skew our judgment.'),
('The Body', 'Bill Bryson', 'Non-Fiction', 449, 'A witty and informative tour of the human body, exploring how it works, what can go wrong, and how remarkable it is.'),
('Freakonomics', 'Steven Levitt and Stephen Dubner', 'Non-Fiction', 242, 'An economist applies data to uncover hidden sides of everyday life and surprising truths.'),
('Born a Crime', 'Trevor Noah', 'Non-Fiction', 289, 'The comedian and Daily Show hosts memoir of growing up mixed-race under apartheid in South Africa.'),
('The Glass Castle', 'Jeannette Walls', 'Non-Fiction', 288, 'A journalist recounts her chaotic but loving childhood with eccentric, itinerant parents who refused to be conventional.');

SET SQL_SAFE_UPDATES = 1;

SELECT COUNT(*) AS total_books FROM books;
SELECT COUNT(*) AS total_users FROM users;
SHOW TABLES;
SELECT * FROM books;
USE dbconnect;
SELECT * FROM users;