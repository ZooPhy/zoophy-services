package edu.asu.zoophy.rest.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import edu.asu.zoophy.rest.genbank.Gene;

/**
 * Maps SQL row to Virus Gene
 * @author devdemetri
 */
public class GeneRowMapper implements RowMapper<Gene> {

	@Override
	public Gene mapRow(ResultSet row, int rowNumber) throws SQLException {
		Gene gene = new Gene();
		gene.setAccession(row.getString("Accession"));
		gene.setName(row.getString("Normalized_Gene_Name"));
		return gene;
	}

}
