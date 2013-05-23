/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.cypherexecutor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.test.GeoffService;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

public class Main
{
    private static final String ENCODING = "UTF-8";

    public static void main( String[] args )
    {
        for ( String name : args )
        {
            executeFile( FileUtils.getFile( name ) );
        }
    }

    private static void executeFile( File file )
    {
        String basename = file.getName();
        Map<String, String> config = new HashMap<String, String>();
        config.put( GraphDatabaseSettings.node_auto_indexing.name(),
                GraphDatabaseSetting.TRUE.toString() );
        config.put( GraphDatabaseSettings.relationship_auto_indexing.name(),
                GraphDatabaseSetting.TRUE.toString() );
        GraphDatabaseService database = new ImpermanentGraphDatabase( config );
        ExecutionEngine engine = new ExecutionEngine( database );
        try
        {
            String name = null;
            String query = "";
            for ( String line : FileUtils.readLines( file, ENCODING ) )
            {
                if ( line.trim()
                        .isEmpty() )
                {
                    executeQuery( basename, name, query, database, engine );
                    name = null;
                    query = "";
                }
                else if ( name == null )
                {
                    name = line;
                }
                else
                {
                    query += line + "\n";
                }
            }
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace();
        }
    }

    private static void executeQuery( String basename, String name,
            String query, GraphDatabaseService database, ExecutionEngine engine )
            throws IOException
    {
        File targetDir = FileUtils.getFile( "target", "snippets", basename );
        String normalizedName = name.toLowerCase()
                .replace( ' ', '-' );
        FileUtils.forceMkdir( targetDir );

        String cypherSnippet = createCypherSnippet( query );
        writeToFile( targetDir, normalizedName + ".query.asciidoc",
                cypherSnippet );

        ExecutionResult result = engine.execute( query );
        String resultSnippet = AsciidocHelper.createQueryResultSnippet( result.toString() );
        writeToFile( targetDir, normalizedName + ".result.asciidoc",
                resultSnippet );

        String graphvizSnippet = AsciidocHelper.createGraphVizDeletingReferenceNode(
                name, database, normalizedName );
        writeToFile( targetDir, normalizedName + ".graph.asciidoc",
                graphvizSnippet );

        String consoleSnippet = createConsoleSnippet( query, database );
        writeToFile( targetDir, normalizedName + ".console.asciidoc",
                consoleSnippet );

        String emptyConsoleSnippet = createConsoleSnippet( query );
        writeToFile( targetDir, normalizedName + ".empty.console.asciidoc",
                emptyConsoleSnippet );
    }

    private static void writeToFile( File targetDir, String filename,
            String content ) throws IOException
    {
        File file = FileUtils.getFile( targetDir, filename );
        FileUtils.writeStringToFile( file, content, ENCODING );
    }

    private static String createConsoleSnippet( String query,
            GraphDatabaseService database )
    {
        String setup = new GeoffService( database ).toGeoff();
        return createConsole( query, setup );
    }

    private static String createConsoleSnippet( String query )
    {
        String setup = "start n=node(*) match n-[r?]->() delete n, r;";
        return createConsole( query, setup );
    }

    private static String createConsole( String query, String setup )
    {
        return "[console]\n----\n" + setup + "\n\n" + query
               + ( query.endsWith( "\n" ) ? "" : "\n" ) + "----\n";
    }

    private static String createCypherSnippet( String query )
    {
        return "[source,cypher]\n----\n" + query
               + ( query.endsWith( "\n" ) ? "" : "\n" ) + "----\n";
    }
}
