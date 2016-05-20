/*
 * Copyright (c) 2016.
 * This file is part of SourceBot.
 *
 * SourceBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SourceBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SourceBot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.walterbarnes.sourcebot.bot.tumblr;

import com.tumblr.jumblr.types.Post;
import net.walterbarnes.sourcebot.bot.search.SearchInclusion;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface ISearchTerm
{
	PostCache getCache();

	@SuppressWarnings ("unused")
	String getSearchTerm();

	@SuppressWarnings ("Duplicates")
	Map<Post, String> getPosts(List<String> blogBlacklist, List<String> tagBlacklist, SearchInclusion rule) throws SQLException;

	List<Post> getPostSet();
}
