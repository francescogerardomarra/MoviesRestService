package com.ubs.ws.movies.api.service.impl;

import static com.ubs.ws.movies.api.error.message.ExceptionMessage.EXCEPTION_MESSAGE_DUPLICATE_MOVIE_ID;
import static com.ubs.ws.movies.api.error.message.ExceptionMessage.EXCEPTION_MESSAGE_MOVIE_NOT_FOUND;

import java.util.LinkedList;
import java.util.List;

import com.ubs.ws.movies.api.error.exception.DuplicateMovieIdException;
import com.ubs.ws.movies.api.error.exception.ResourceNotFoundException;
import com.ubs.ws.movies.api.repository.MovieRepository;
import com.ubs.ws.movies.api.service.MovieService;
import com.ubs.ws.movies.pojo.bean.Comment;
import com.ubs.ws.movies.pojo.bean.Movie;
import com.ubs.ws.movies.pojo.dto.BaseCommentDTO;
import com.ubs.ws.movies.pojo.dto.MovieDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultMovieService implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Override
    public void addMovie(MovieDTO movieDTO) throws DuplicateMovieIdException {

        /* CAVEAT
        In Spring JPA, SimpleJpaRepository.save() is implemented as follows:

                    @Transactional
                    public <S extends T> S save(S entity) {
                        if(this.entityInformation.isNew(entity)) {
                            this.em.persist(entity);
                            return entity;
                        } else {
                            return this.em.merge(entity);
                        }
                    }

        this is trying to merge if the record already exists; thus if the ID already exists in the database, the insertion
        is actually treated as an update.

        A workaround is to use an automatic generation for IDs (e.g. see the @Entity Comment where we use @GeneratedValue on
        commentId) which will prevent at anytime to have two identical IDs. However, as for the @Entity Movie, it is more
        comfortable to choose the movieId manually for practical purposes when testing the application with a client.

        At this point, to avoid the "merge" behaviour, we have to make a check if the passed ID is already occupied by another
        movie, and reject the insertion in that case. */

        Movie duplicateMovie = movieRepository.findOne(movieDTO.getMovieId());

        if (duplicateMovie != null)
            throw new DuplicateMovieIdException(movieDTO.getMovieId(), EXCEPTION_MESSAGE_DUPLICATE_MOVIE_ID);

        long movieId = movieDTO.getMovieId();
        String title = movieDTO.getTitle();
        String description = movieDTO.getDescription();
        Movie movie = new Movie(movieId, title, description, null);
        movieRepository.save(movie);
    }

    @Override
    public MovieDTO getMovieById(long movieId) throws ResourceNotFoundException {
        Movie movie = movieRepository.findOne(movieId);

        if (movie == null)
            throw new ResourceNotFoundException(movieId, EXCEPTION_MESSAGE_MOVIE_NOT_FOUND);

        String title = movie.getTitle();
        String description = movie.getDescription();
        List<BaseCommentDTO> baseCommentDTOList = new LinkedList<>();

        List<Comment> comments = movie.getComments();
        comments.forEach(comment -> {
            BaseCommentDTO baseCommentDTO = new BaseCommentDTO(comment.getUsername(), comment.getMessage());
            baseCommentDTOList.add(baseCommentDTO);
        });

        return new MovieDTO(movieId, title, description, baseCommentDTOList);
    }

/*    @Override
    public List<Movie> getAllMovies() {
        List<Movie> movies = new LinkedList<>();
        movieRepository.findAll()
                .forEach(movies::add);
        return movies;
    }*/
}
