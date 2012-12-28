package org.exoplatform.commons.search.driver.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.commons.search.Search;
import org.exoplatform.commons.search.SearchResult;
import org.exoplatform.commons.search.util.QueryParser;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;

public class JcrNodeSearch implements Search {
      
  @Override
  public Collection<SearchResult> search(String query) {
    if(query.startsWith("SELECT")) return sqlExec(query); // sql mode (for testing)
    Collection<SearchResult> results = new ArrayList<SearchResult>();
    try {
      QueryParser parser = new QueryParser(query); 
      parser = parser.parseFor("orderby");
      String orderby = parser.getResults().isEmpty() ? "jcr:score()" : parser.getResults().get(0);
      
      Collection<JcrSearchResult> jcrResults = JcrSearchService.search("repository=repository workspace=collaboration from=nt:base where=CONTAINS(*,'${query}') " + query);
      for(JcrSearchResult jcrResult: jcrResults) {
        String nodeUrl = jcrResult.getRepository() + "/" + jcrResult.getWorkspace() + jcrResult.getPath();
        SearchResult result = new SearchResult("jcrNode", "/rest/jcr/" + nodeUrl);
        String score = String.valueOf(jcrResult.getScore());
        result.setTitle(nodeUrl + " (score = " + score + ")");
        result.setExcerpt(jcrResult.getExcerpt());
        String orderbyValue = orderby.equals("jcr:score()") ? score : (String)jcrResult.getProperty(orderby);
        result.setDetail(orderby + " = " + orderbyValue);
        result.setAvatar("/eXoWCMResources/skin/DefaultSkin/skinIcons/48x48/icons/NodeTypes/default.gif");

        results.add(result);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return results;
  }

  
  private static Collection<SearchResult> sqlExec(String sql) {
    System.out.format("[UNIFIED SEARCH] JcrSearchService.search()\nsql = %s\n", sql);
    Collection<SearchResult> results = new ArrayList<SearchResult>();
    String orderby = "jcr:score()";
    Matcher matcher = Pattern.compile("ORDER BY\\s+([\\S]+)").matcher(sql);
    if(matcher.find()) {
      orderby = matcher.group(1);
    }

    try {
      RepositoryService repositoryService = (RepositoryService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
      for(RepositoryEntry repositoryEntry:repositoryService.getConfig().getRepositoryConfigurations()){
        String repoName = repositoryEntry.getName();
        System.out.format("[UNIFIED SEARCH]: searching repository '%s'...\n", repoName);
        
        ManageableRepository repository = repositoryService.getRepository(repoName);
        List<SearchResult> result = new ArrayList<SearchResult>();    
        
        for(String workspaceName:repository.getWorkspaceNames()){
          System.out.format("[UNIFIED SEARCH]: searching workspace '%s'...\n", workspaceName);
          
          Session session = repository.login(workspaceName);
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          Query jcrQuery = queryManager.createQuery(sql, Query.SQL);
          QueryResult queryResult = jcrQuery.execute();
          
          RowIterator rit = queryResult.getRows();
          while(rit.hasNext()){
            Row row = rit.nextRow();
            String path = row.getValue("jcr:path").getString();
            
            String collection = repository.getConfiguration().getName() + "/" + session.getWorkspace().getName();
            String jcrType = row.getValue("jcr:primaryType").getString();
            if(jcrType.equals("nt:resource")){
              path = path.substring(0, path.lastIndexOf("/jcr:content"));
            }

            String score = String.valueOf(row.getValue("jcr:score").getLong());
            SearchResult resultItem = new SearchResult(jcrType, "/rest/jcr/" + collection + path);
            resultItem.setTitle(collection + path + " (score = " + score + ")");
            Value excerpt = row.getValue("rep:excerpt()");
            resultItem.setExcerpt(null!=excerpt?excerpt.getString():"");
            String orderbyValue = orderby.equals("jcr:score()") ? score : "&lt;Click the icon to see all properties of this node&gt;";
            resultItem.setDetail(orderby + " = " + orderbyValue);
            resultItem.setAvatar("/eXoWCMResources/skin/DefaultSkin/skinIcons/48x48/icons/NodeTypes/default.gif");

            result.add(resultItem);
          }
        }

        results.addAll(result);
      }      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return results;
  }

}
