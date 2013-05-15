#ifndef MUSCLE_EXCEPTION_HPP
#define MUSCLE_EXCEPTION_HPP

#include <exception>
#include <stdexcept>
#include <string>
#include <errno.h>

#include "logger.hpp"

namespace muscle {

class muscle_exception : public std::runtime_error {
public:
    const int error_code;

    muscle_exception(const muscle_exception& ex) : std::runtime_error(ex.what()), error_code(ex.error_code) {}
    muscle_exception(const std::exception& ex) : std::runtime_error(ex.what()), error_code(errno) {}
	muscle_exception (std::string msg) throw() : std::runtime_error(msg), error_code(errno) {}
	muscle_exception (std::string msg, int code) throw() : std::runtime_error(msg), error_code(code)
    {
		logger::severe(msg.c_str());
	}
};

}

#endif
