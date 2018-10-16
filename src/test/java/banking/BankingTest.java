package banking;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;


public class BankingTest {
	private static DataSource myDataSource; // La source de données à utiliser
	private static Connection myConnection ;	
	private BankingDAO myDAO;
	
	@Before
	public void setUp() throws SQLException, IOException, SqlToolError {
		// On crée la connection vers la base de test "in memory"
		myDataSource = getDataSource();
		myConnection = myDataSource.getConnection();
		// On initialise la base avec le contenu d'un fichier de test
		String sqlFilePath = BankingTest.class.getResource("testdata.sql").getFile();
		SqlFile sqlFile = new SqlFile(new File(sqlFilePath));
		sqlFile.setConnection(myConnection);
		sqlFile.execute();
		sqlFile.closeReader();	
		// On crée l'objet à tester
		myDAO = new BankingDAO(myDataSource);
	}
	
	@After
	public void tearDown() throws SQLException {
		myConnection.close();		
		myDAO = null; // Pas vraiment utile
	}

	
	@Test
	public void findExistingCustomer() throws SQLException {
		float balance = myDAO.balanceForCustomer(0);
		// attention à la comparaison des nombres à virgule flottante !
		assertEquals("Balance incorrecte !", 100.0f, balance, 0.001f);
	}

	@Test
	public void successfulTransfer() throws Exception {
		float amount = 10.0f;
		int fromCustomer = 0;
		int toCustomer = 1;
		float before0 = myDAO.balanceForCustomer(fromCustomer);
		float before1 = myDAO.balanceForCustomer(toCustomer);
		myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
		// Les balances doivent avoir été mises à jour dans les 2 comptes
		assertEquals("Balance incorrecte !", before0 - amount, myDAO.balanceForCustomer(fromCustomer), 0.001f);
		assertEquals("Balance incorrecte !", before1 + amount, myDAO.balanceForCustomer(toCustomer), 0.001f);				
	}
	
        // Le solde du compte débité devient négatif 
        // throw new IllegalArgumentException("Le solde ne doit pas devenir négatif après la transaction")
        @Test( expected = IllegalArgumentException.class )
        public void totalBecomingNegative() throws SQLException, Exception {
            float amount = 150.0f;  // le customer 0 à 100€, le solde deviendra négatif après la transaction
            int fromCustomer = 0;
            int toCustomer = 1;
            float before0 = myDAO.balanceForCustomer(fromCustomer);
            float before1 = myDAO.balanceForCustomer(toCustomer);
            myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
            
            // La balance ne doit pas avoir changé !!           
            // attention à la comparaison des nombres à virgule flottante !
            assertEquals("Solde insuffisant !", before0, myDAO.balanceForCustomer(fromCustomer), 0.001f);
            assertEquals("Solde insuffisant !", before1, myDAO.balanceForCustomer(fromCustomer), 0.001f);
        }
        
        // Que doit-il se passer si le compte débité ou le compte crédité n'existent pas ?
        @Test( expected = IllegalArgumentException.class )
        public void sendingAccountsDoesNotExist() throws SQLException, Exception {
            float amount = 150.0f;
            int fromCustomer = 10; // Le client 10 n'existe pas
            int result = -1;
            
            // On vérifie que l'ID du compte 
            String sql = "SELECT ID AS NUM FROM Customer";
            try (   Connection connection = myDataSource.getConnection(); // Ouvrir une connexion
                    Statement stmt = connection.createStatement(); // On crée un statement pour exécuter une requête
                    ResultSet rs = stmt.executeQuery(sql) // Un ResultSet pour parcourir les enregistrements du résultat
            ) {
                    boolean correct = false;
                    while (rs.next()) { // Pas la peine de faire while, il y a 1 seul enregistrement
                            // On récupère le champ NUMBER de l'enregistrement courant
                            result = rs.getInt("NUM");

                            /* Vérification de l'existence du compte client */
                            if (result == fromCustomer) {
                                correct = true;
                            }
                    }
                    
                    // On a pas trouvé le compte du client
                    if (!correct) {
                        throw new IllegalArgumentException("Le compte demandé n'existe pas.");   // EXCEPTION que l'on doit traiter
                    }

            } catch (SQLException ex) {
                    throw new SQLException("Erreur lors de l'accès à la base de données.");
            }
                
            myDAO.balanceForCustomer(fromCustomer);
        }
        
        // Que doit-il se passer si le compte débité ou le compte crédité n'existent pas ?
        @Test( expected = IllegalArgumentException.class )
        public void receivingAccountsDoesNotExist() throws SQLException, Exception {
            float amount = 150.0f;
            int toCustomer = 11;   // Le client 11 n'existe pas
            int result = -1;
            
            // On vérifie que l'ID du compte 
            String sql = "SELECT ID AS NUM FROM Customer";
            try (   Connection connection = myDataSource.getConnection(); // Ouvrir une connexion
                    Statement stmt = connection.createStatement(); // On crée un statement pour exécuter une requête
                    ResultSet rs = stmt.executeQuery(sql) // Un ResultSet pour parcourir les enregistrements du résultat
            ) {
                    boolean correct = false;
                    while (rs.next()) { // Pas la peine de faire while, il y a 1 seul enregistrement
                            // On récupère le champ NUMBER de l'enregistrement courant
                            result = rs.getInt("NUM");

                            /* Vérification de l'existence du compte client */
                            if (result == toCustomer) {
                                correct = true;
                            }
                    }
                    
                    // On a pas trouvé le compte du client
                    if (!correct) {
                        throw new IllegalArgumentException("Le compte demandé n'existe pas.");   // EXCEPTION que l'on doit traiter
                    }

            } catch (SQLException ex) {
                    throw new SQLException("Erreur lors de l'accès à la base de données.");
            }
                
            myDAO.balanceForCustomer(toCustomer);
        }
        
        // Maintenant on teste avec un compte client valide
        @Test
        public void goodCustomerAccount() throws SQLException, Exception {
            float amount = 150.0f;
            int toCustomer = 1;   // Le client 11 n'existe pas
            int result = -1;
            
                        // On vérifie que l'ID du compte 
            String sql = "SELECT ID AS NUM FROM Customer";
            try (   Connection connection = myDataSource.getConnection(); // Ouvrir une connexion
                    Statement stmt = connection.createStatement(); // On crée un statement pour exécuter une requête
                    ResultSet rs = stmt.executeQuery(sql) // Un ResultSet pour parcourir les enregistrements du résultat
            ) {
                    boolean correct = false;
                    while (rs.next()) { // Pas la peine de faire while, il y a 1 seul enregistrement
                            // On récupère le champ NUMBER de l'enregistrement courant
                            result = rs.getInt("NUM");

                            /* Vérification de l'existence du compte client */
                            if (result == toCustomer) {
                                correct = true;
                            }
                    }
                    
                    // On a pas trouvé le compte du client
                    if (!correct) {
                        throw new IllegalArgumentException("Le compte demandé n'existe pas.");   // EXCEPTION que l'on doit traiter
                    }

            } catch (SQLException ex) {
                    throw new SQLException("Erreur lors de l'accès à la base de données.");
            }
                
            myDAO.balanceForCustomer(toCustomer);
        }
        
	public static DataSource getDataSource() throws SQLException {
		org.hsqldb.jdbc.JDBCDataSource ds = new org.hsqldb.jdbc.JDBCDataSource();
		ds.setDatabase("jdbc:hsqldb:mem:testcase;shutdown=true");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}	
}
